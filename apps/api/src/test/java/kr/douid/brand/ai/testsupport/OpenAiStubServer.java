package kr.douid.brand.ai.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** 실제 인터넷을 사용하지 않는 Count·생성 HTTP 대역 */
public final class OpenAiStubServer implements AutoCloseable {
    @FunctionalInterface public interface Handler {
        /** 대역 연결 동작
         * @param exchange 현재 요청
         * @throws Exception 지연·단절 대역 실패
         */
        void handle(HttpExchange exchange) throws Exception;
    }
    private final HttpServer server;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    public final List<String> countRequests = new CopyOnWriteArrayList<>();
    public final List<String> generationRequests = new CopyOnWriteArrayList<>();
    public volatile Handler count = e -> reply(e, 200, "application/json", "{\"object\":\"response.input_tokens\",\"input_tokens\":100}");
    public volatile Handler generation = e -> reply(e, 200, "text/event-stream", success("안내😀"));

    /** loopback 전용 대역 시작
     * @throws IOException 서버 생성 실패
     */
    public OpenAiStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(workers);
        server.createContext("/v1/responses/input_tokens", e -> serve(e, countRequests, count));
        server.createContext("/v1/responses", e -> serve(e, generationRequests, generation));
        server.start();
    }
    /** 생성 Endpoint
     * @return loopback URI
     */
    public URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses"); }

    private void serve(HttpExchange e, List<String> requests, Handler handler) throws IOException {
        try (e) {
            requests.add(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            handler.handle(e);
        } catch (Exception ignored) { /* 단절 대역도 동일하게 종료 */ }
    }
    /** 대역 응답 전송
     * @param e 현재 연결
     * @param status HTTP 상태
     * @param type 콘텐츠 유형
     * @param body 대역 본문
     * @throws IOException 쓰기 실패
     */
    public static void reply(HttpExchange e, int status, String type, String body) throws IOException {
        e.getResponseHeaders().set("Content-Type", type);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.sendResponseHeaders(status, bytes.length);
        e.getResponseBody().write(bytes);
    }
    /** 대역 SSE 프레임
     * @param json 이벤트 JSON
     * @return data 프레임
     */
    public static String event(String json) { return "data: " + json + "\n\n"; }
    /** 최소 정상 이벤트 흐름
     * @param text JSON 이스케이프가 불필요한 대역 텍스트
     * @return 정상 완료 SSE
     */
    public static String success(String text) {
        return event("{\"type\":\"response.created\",\"sequence_number\":0,\"response\":{\"id\":\"r1\",\"status\":\"in_progress\"}}")
                + event("{\"type\":\"response.output_item.added\",\"sequence_number\":1,\"output_index\":0,\"item\":{\"id\":\"m1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[]}}")
                + event("{\"type\":\"response.content_part.added\",\"sequence_number\":2,\"item_id\":\"m1\",\"output_index\":0,\"content_index\":0,\"part\":{\"type\":\"output_text\",\"text\":\"\"}}")
                + event("{\"type\":\"response.output_text.delta\",\"sequence_number\":3,\"item_id\":\"m1\",\"output_index\":0,\"content_index\":0,\"delta\":\""+text+"\"}")
                + event("{\"type\":\"response.output_text.done\",\"sequence_number\":4,\"item_id\":\"m1\",\"output_index\":0,\"content_index\":0,\"text\":\""+text+"\"}")
                + event("{\"type\":\"response.completed\",\"sequence_number\":5,\"response\":{\"id\":\"r1\",\"status\":\"completed\",\"output\":[{\"id\":\"m1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\""+text+"\"}]}],\"usage\":{\"input_tokens\":100,\"output_tokens\":2,\"total_tokens\":102}}}");
    }
    /** 대역 HTTP와 테스트 worker 종료 */
    @Override public void close() { server.stop(0); workers.shutdownNow(); }
}
