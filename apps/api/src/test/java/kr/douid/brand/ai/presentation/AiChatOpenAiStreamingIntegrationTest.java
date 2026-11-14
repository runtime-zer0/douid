package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import kr.douid.brand.ai.application.port.*;
import kr.douid.brand.ai.infrastructure.config.OpenAiProperties;
import kr.douid.brand.ai.infrastructure.openai.OpenAiResponsesGateway;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AiChatOpenAiStreamingIntegrationTest.Provider.class)
class AiChatOpenAiStreamingIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired OpenAiStubServer stub;

    @BeforeEach void resetProvider() {
        stub.countRequests.clear(); stub.generationRequests.clear();
    }

    @Test @Timeout(15)
    void 두_요청의_Provider_상태와_텍스트는_섞이지않음() throws Exception {
        var number=new java.util.concurrent.atomic.AtomicInteger();
        stub.generation=e->OpenAiStubServer.reply(e,200,"text/event-stream",OpenAiStubServer.success("answer"+number.incrementAndGet()));
        try(var client=HttpClient.newHttpClient()) {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                    .header("Content-Type","application/json").timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"overlap\"}")).build();
            var first=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
            var second=client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
            String one=first.get(10,TimeUnit.SECONDS).body(),two=second.get(10,TimeUnit.SECONDS).body();
            for(String body:new String[]{one,two}) {
                assertThat(body).contains("event:completed").doesNotContain("event:error");
                assertThat(body.contains("answer1") ^ body.contains("answer2")).isTrue();
            }
            assertThat(one.contains("answer1")).isNotEqualTo(two.contains("answer1"));
            assertThat(one.substring(0,one.indexOf("message_delta"))).isNotEqualTo(two.substring(0,two.indexOf("message_delta")));
        }
    }

    @Test @Timeout(15)
    void Provider완료_전_첫Delta와_완료후_유일terminal() throws Exception {
        var release = new CountDownLatch(1);
        String sse = OpenAiStubServer.success("첫😀");
        int boundary = sse.indexOf("data: {\"type\":\"response.output_text.done\"");
        stub.generation = e -> {
            e.getResponseHeaders().set("Content-Type", "text/event-stream");
            e.sendResponseHeaders(200, 0);
            e.getResponseBody().write(sse.substring(0, boundary).getBytes(StandardCharsets.UTF_8));
            e.getResponseBody().flush();
            release.await(10, TimeUnit.SECONDS);
            e.getResponseBody().write(sse.substring(boundary).getBytes(StandardCharsets.UTF_8));
        };
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/ai-chat/stream"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hi\"}")).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/event-stream");
            assertThat(response.headers().firstValue("X-Accel-Buffering")).contains("no");
            try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                StringBuilder first = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    first.append(line).append('\n');
                    if (first.toString().contains("event:message_delta") && line.isEmpty()) break;
                }
                assertThat(first.toString()).contains("event:started", "첫😀").doesNotContain("event:completed");
                assertThat(release.getCount()).isEqualTo(1);
                release.countDown();
                String rest = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                assertThat(rest).contains("event:completed").doesNotContain("message_delta", "event:error");
            }
        } finally { release.countDown(); }
        assertThat(stub.countRequests).hasSize(1);
        assertThat(stub.generationRequests).hasSize(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Provider {
        @Bean(destroyMethod = "close") OpenAiStubServer openAiStub() throws IOException { return new OpenAiStubServer(); }
        @Bean(destroyMethod = "close") HttpClient providerHttpClient() {
            return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        }
        @Bean @Primary LlmGateway providerGateway(OpenAiStubServer stub, HttpClient providerHttpClient, ChatStreamExecutor executor) {
            var p = new OpenAiProperties(); p.setEnabled(true); p.setApiKey("FAKE_KEY_CANARY");
            return OpenAiResponsesGateway.forLoopbackTest(p, providerHttpClient, executor, new SimpleMeterRegistry(), stub.endpoint());
        }
    }
}
