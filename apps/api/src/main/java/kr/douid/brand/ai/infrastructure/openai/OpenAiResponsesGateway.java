package kr.douid.brand.ai.infrastructure.openai;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;
import io.micrometer.core.instrument.MeterRegistry;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.infrastructure.config.OpenAiProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import static kr.douid.brand.ai.application.LlmIntegrationException.Kind.*;
import kr.douid.brand.ai.application.LlmIntegrationException.Kind;

/** 요청당 Count 1회·생성 최대 1회의 서버 전용 Responses adapter */
public final class OpenAiResponsesGateway implements LlmGateway {
    private final OpenAiProperties properties;
    private final HttpClient client;
    private final ChatStreamExecutor executor;
    private final MeterRegistry metrics;
    private final URI endpoint;
    private final OpenAiRequestComposer composer;

    /** 운영 HTTPS Endpoint에 연결
     * @param properties 서버 설정
     * @param client 공유 redirect 금지 client
     * @param executor 기존 deadline 예약 경계
     * @param metrics 비식별 관측 registry
     */
    public OpenAiResponsesGateway(OpenAiProperties properties, HttpClient client, ChatStreamExecutor executor, MeterRegistry metrics) {
        this(properties, client, executor, metrics, URI.create("https://api.openai.com/v1/responses"));
    }

    private OpenAiResponsesGateway(OpenAiProperties properties, HttpClient client, ChatStreamExecutor executor,
                                  MeterRegistry metrics, URI endpoint) {
        if (client.followRedirects() != HttpClient.Redirect.NEVER) throw new IllegalArgumentException("Redirects forbidden");
        this.properties = properties;
        this.client = client;
        this.executor = executor;
        this.metrics = metrics;
        this.endpoint = endpoint;
        this.composer = new OpenAiRequestComposer(properties);
    }

    /** 외부 origin을 허용하지 않는 로컬 테스트 연결
     * @param properties 가짜 credential 설정
     * @param client 테스트 소유 client
     * @param executor 기존 예약 경계
     * @param metrics 테스트 registry
     * @param endpoint 숫자 loopback 생성 URI
     * @return 로컬 대역 전용 adapter
     * @throws IllegalArgumentException loopback 이외의 URI
     */
    public static OpenAiResponsesGateway forLoopbackTest(OpenAiProperties properties, HttpClient client,
            ChatStreamExecutor executor, MeterRegistry metrics, URI endpoint) {
        if (!"http".equals(endpoint.getScheme()) || !"127.0.0.1".equals(endpoint.getHost())
                || endpoint.getPort() < 1 || !"/v1/responses".equals(endpoint.getPath())
                || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null)
            throw new IllegalArgumentException("Invalid loopback test endpoint");
        return new OpenAiResponsesGateway(properties, client, executor, metrics, endpoint);
    }

    /** 외부 호출 없는 로컬 snapshot 준비
     * @param command 기존 검증을 통과한 불신 입력
     * @return 요청별 단일 실행 source
     */
    @Override public LlmStream prepare(AiChatCommand command) {
        if (!properties.ready()) throw new AiChatException.Unavailable();
        return new Source(composer.compose(command));
    }

    private final class Source implements LlmStream {
        private final OpenAiRequestComposer.Requests requests;
        private final AtomicBoolean run = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private boolean finished;
        private long timerVersion;
        private final AtomicReference<Kind> timeout = new AtomicReference<>();
        private final AtomicReference<OpenAiBodySubscriber> subscriber = new AtomicReference<>();
        private volatile Thread worker;
        private Future<?> timer;
        private String stage = "count";
        private OpenAiEventReader reader;

        Source(OpenAiRequestComposer.Requests requests) { this.requests = requests; }

        /** 단일 worker의 Count 검증과 순차 생성
         * @param delta 기존 순차 텍스트 소비자
         * @throws Exception 취소·제공자·소비자 실패
         */
        @Override public void run(Consumer<String> delta) throws Exception {
            if (!run.compareAndSet(false, true)) throw new IllegalStateException("Stream already run");
            worker = Thread.currentThread();
            Kind failure = null;
            boolean stageRecorded = false;
            long started = System.nanoTime();
            try {
                checkCancelled();
                arm(properties.getCountTimeout(), () -> expire(COUNT_TIMEOUT));
                try (var body = send(URI.create(endpoint + "/input_tokens"), requests.count(),
                        properties.getCountTimeout(), properties.getMaxCountBodyBytes(), "application/json")) {
                    JsonNode count;
                    try { count = OpenAiEventReader.JSON.readTree(body.readAllBytes()); }
                    catch (JacksonException e) { throw new LlmIntegrationException(PROTOCOL); }
                    if (count == null || !"response.input_tokens".equals(OpenAiEventReader.text(count, "object")))
                        throw new LlmIntegrationException(PROTOCOL);
                    long tokens = OpenAiEventReader.integer(count, "input_tokens");
                    if (tokens > properties.getMaxInputTokens() || tokens > 1_050_000L - properties.getMaxOutputTokens())
                        throw new LlmIntegrationException(INPUT_BUDGET_EXCEEDED);
                    metrics.summary("ai.provider.admission.tokens").record(tokens);
                }
                disarm();
                recordStage(started, null);
                stageRecorded = true;
                checkCancelled();
                stage = "generation";
                stageRecorded = false;
                started = System.nanoTime();
                arm(properties.getResponseHeaderTimeout(), () -> expire(HEADER_TIMEOUT));
                try (var body = send(endpoint, requests.generation(), properties.getResponseHeaderTimeout(),
                        properties.getMaxProviderBodyBytes(), "text/event-stream")) {
                    disarm();
                    checkCancelled();
                    watchIdle();
                    reader = new OpenAiEventReader(properties.getMaxProviderEventBytes());
                    reader.read(body, text -> { checkCancelled(); delta.accept(text); });
                    checkCancelled();
                }
            } catch (LlmIntegrationException e) {
                failure = timeout.get() != null ? timeout.get() : e.kind();
                throw new LlmIntegrationException(failure);
            } catch (IOException | InterruptedException e) {
                failure = transportFailure(e);
                throw new LlmIntegrationException(failure);
            } catch (RuntimeException e) {
                failure = timeout.get() != null ? timeout.get() : cancelled.get() ? CLIENT_CANCELLED : PROVIDER_FAILED;
                if (timeout.get() != null) throw new LlmIntegrationException(timeout.get());
                if (e instanceof StreamSink.LimitExceeded || e instanceof StreamSink.Disconnected) throw e;
                throw new LlmIntegrationException(failure);
            } finally {
                synchronized (this) { finished = true; disarm(); }
                var current = subscriber.get();
                if (current != null) current.cancel();
                worker = null;
                if (!stageRecorded) recordStage(started, failure);
                if (stage.equals("generation")) recordUsage(failure);
            }
        }

        private InputStream send(URI uri, byte[] payload, Duration deadline, int maxBytes, String expectedType)
                throws IOException, InterruptedException {
            checkCancelled();
            var next = new OpenAiBodySubscriber(maxBytes);
            subscriber.set(next);
            if (cancelled.get()) next.cancel();
            checkCancelled();
            var request = HttpRequest.newBuilder(uri).timeout(deadline)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json").header("Accept", expectedType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
            metrics.counter("ai.provider.attempts", "stage", stage).increment();
            var response = client.send(request, info -> next);
            try {
                checkCancelled();
                int status = response.statusCode();
                if (status != 200) throw new LlmIntegrationException(switch (status) {
                    case 401, 403 -> AUTHENTICATION;
                    case 429 -> RATE_LIMIT;
                    case 500, 502, 503, 504 -> UNAVAILABLE;
                    default -> PROVIDER_FAILED;
                });
                String type = response.headers().firstValue("Content-Type").orElse("")
                        .split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
                if (!expectedType.equals(type)) throw new LlmIntegrationException(PROTOCOL);
                return response.body();
            } catch (RuntimeException e) {
                next.cancel();
                response.body().close();
                throw e;
            }
        }
        private Kind transportFailure(Exception e) {
            if (timeout.get() != null) return timeout.get();
            if (cancelled.get()) return CLIENT_CANCELLED;
            if (subscriber.get() != null && subscriber.get().exceeded()) return PROVIDER_RESOURCE_LIMIT;
            if (e instanceof HttpConnectTimeoutException) return CONNECT_TIMEOUT;
            if (e instanceof HttpTimeoutException) return stage.equals("count") ? COUNT_TIMEOUT : HEADER_TIMEOUT;
            return CONNECTION;
        }
        private void checkCancelled() {
            if (timeout.get() != null) throw new LlmIntegrationException(timeout.get());
            if (cancelled.get() || closed.get()) throw new LlmIntegrationException(CLIENT_CANCELLED);
        }
        private synchronized void arm(Duration delay, Runnable callback) {
            if (!cancelled.get() && !closed.get() && !finished) {
                long version = ++timerVersion;
                timer = executor.schedule(() -> {
                    synchronized (this) {
                        if (version == timerVersion && !finished && !cancelled.get() && !closed.get()) callback.run();
                    }
                }, delay);
            }
        }
        private synchronized void disarm() {
            timerVersion++;
            if (timer != null) { timer.cancel(false); timer = null; }
        }
        private void expire(Kind why) { if (timeout.compareAndSet(null, why)) cancel(); }
        private void watchIdle() {
            watchIdle(properties.getReadIdleTimeout());
        }
        private void watchIdle(Duration delay) {
            arm(delay, () -> {
                var current = subscriber.get();
                long remaining = current == null ? 0 : properties.getReadIdleTimeout().toNanos()
                        - (System.nanoTime() - current.lastReceivedAt());
                if (remaining <= 0) expire(READ_IDLE_TIMEOUT);
                else watchIdle(Duration.ofNanos(remaining));
            });
        }
        /** 공유 client를 닫지 않는 상위 취소 요청 */
        @Override public void cancel() {
            cancelled.set(true);
            var current = subscriber.get(); if (current != null) current.cancel();
            Thread executing = worker; if (executing != null) executing.interrupt();
        }
        /** worker의 본문 정리 후 반복 가능한 source 종료 */
        @Override public void close() { if (closed.compareAndSet(false, true)) { cancel(); disarm(); } }
        private void recordStage(long started, Kind failure) {
            String outcome = failure == null ? "success" : failure.name().toLowerCase(Locale.ROOT);
            metrics.counter("ai.provider.results", "stage", stage, "outcome", outcome).increment();
            metrics.timer("ai.provider.duration", "stage", stage, "outcome", outcome)
                    .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
        private void recordUsage(Kind failure) {
            String outcome = failure == null ? "success" : failure.name().toLowerCase(Locale.ROOT);
            var usage = reader == null ? null : reader.usage();
            try {
                if (usage == null) throw new LlmIntegrationException(PROTOCOL);
                long input = OpenAiEventReader.integer(usage, "input_tokens");
                long output = OpenAiEventReader.integer(usage, "output_tokens");
                long total = OpenAiEventReader.integer(usage, "total_tokens");
                if (input > Long.MAX_VALUE - output || input + output != total) throw new LlmIntegrationException(PROTOCOL);
                metrics.summary("ai.provider.tokens", "kind", "input", "outcome", outcome).record(input);
                metrics.summary("ai.provider.tokens", "kind", "output", "outcome", outcome).record(output);
                metrics.summary("ai.provider.tokens", "kind", "total", "outcome", outcome).record(total);
                metrics.counter("ai.provider.usage", "known", "true", "outcome", outcome).increment();
            } catch (LlmIntegrationException e) {
                metrics.counter("ai.provider.usage", "known", "false", "outcome", outcome).increment();
            }
        }
    }
}
