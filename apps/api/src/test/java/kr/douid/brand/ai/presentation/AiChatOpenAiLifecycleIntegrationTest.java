package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "app.ai-chat.execution-timeout=1500ms","app.ai-chat.emitter-timeout=3s","app.ai-chat.rate-limit.requests=100"})
@Import(AiChatOpenAiStreamingIntegrationTest.Provider.class)
@Timeout(15)
class AiChatOpenAiLifecycleIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired OpenAiStubServer stub;

    HttpRequest request() {
        return HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                .timeout(Duration.ofSeconds(8)).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hello\"}")).build();
    }
    void continuous(CountDownLatch closed) {
        stub.generation=e->{
            try {
                e.getResponseHeaders().set("Content-Type","text/event-stream");e.sendResponseHeaders(200,0);
                String normal=OpenAiStubServer.success("tick");
                e.getResponseBody().write(normal.substring(0,normal.indexOf("data: {\"type\":\"response.output_text.done")).getBytes(StandardCharsets.UTF_8));
                e.getResponseBody().flush();
                for(int seq=4;seq<300;seq++) {
                    Thread.sleep(20);
                    e.getResponseBody().write(OpenAiStubServer.event("{\"type\":\"response.output_text.delta\",\"sequence_number\":"+seq+
                            ",\"item_id\":\"m1\",\"output_index\":0,\"content_index\":0,\"delta\":\"tick\"}").getBytes(StandardCharsets.UTF_8));
                    e.getResponseBody().flush();
                }
            } finally {closed.countDown();}
        };
    }
    @Test void 지속적인_Delta도_전체기한을_연장하지않음() throws Exception {
        var closed=new CountDownLatch(1);continuous(closed);
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.body()).contains("message_delta","STREAM_TIMEOUT").doesNotContain("event:completed");
            assertThat(closed.await(3,TimeUnit.SECONDS)).isTrue();
        }
    }
    @Test void 실제_브라우저측_연결종료_뒤_상위HTTP도_정리() throws Exception {
        var closed=new CountDownLatch(1);continuous(closed);
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofInputStream());
            try(var reader=new BufferedReader(new InputStreamReader(response.body(),StandardCharsets.UTF_8))) {
                String line;
                while((line=reader.readLine())!=null) if(line.contains("tick")) break;
                assertThat(line).isNotNull();
            }
            assertThat(closed.await(4,TimeUnit.SECONDS)).isTrue();
        }
    }
    @Test void 부분답변_이후_incomplete는_completed없이_유일error() throws Exception {
        stub.generation=e->OpenAiStubServer.reply(e,200,"text/event-stream",OpenAiStubServer.success("partial")
                .replace("response.completed","response.incomplete").replace("\"status\":\"completed\"","\"status\":\"incomplete\""));
        try(var client=HttpClient.newHttpClient()) {
            String body=client.send(request(),HttpResponse.BodyHandlers.ofString()).body();
            assertThat(body).contains("partial","STREAM_FAILED").doesNotContain("event:completed");
            assertThat(body.split("event:error",-1)).hasSize(2);
        }
    }
}
