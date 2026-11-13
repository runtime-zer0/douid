package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AiChatTestConfiguration.class)
class AiChatStreamingIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired ControlledLlmGateway gateway;
    @Test @org.junit.jupiter.api.Timeout(15)
    void source_완료전_첫_Delta_수신과_이후_순서_UTF8_EOF() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("첫😀\n\"조각");stream.awaitRelease();delta.accept("끝");};
        try(var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                    .header("Content-Type","application/json").header("Accept","text/event-stream")
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hi\"}")).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            try(var reader=new BufferedReader(new InputStreamReader(response.body(),StandardCharsets.UTF_8))) {
                var first=new StringBuilder();String line;
                while((line=reader.readLine())!=null) {first.append(line).append('\n');
                    if(first.toString().contains("event:message_delta") && line.isEmpty()) break;}
                assertThat(first.toString()).contains("event:started","event:message_delta","첫😀");
                assertThat(gateway.last.release.getCount()).isEqualTo(1);
                gateway.last.release.countDown();
                String rest=reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                assertThat(rest).containsSubsequence("event:message_delta","끝","event:completed");
                assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
            } finally {if(gateway.last!=null)gateway.last.release.countDown();}
        }
    }
    @Test @org.junit.jupiter.api.Timeout(20)
    void 겹치는_연결도_서로_독립된_초기화와_종료() throws Exception {
        gateway.behavior=(stream,delta)->{delta.accept("open");stream.awaitRelease();};
        try(var client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            for(int iteration=0;iteration<6;iteration++) {
                var inputs=new java.util.ArrayList<InputStream>();
                try {
                    for(int i=0;i<2;i++) {
                        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                                .header("Content-Type","application/json").timeout(Duration.ofSeconds(3))
                                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"overlap\"}")).build();
                        var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
                        assertThat(response.statusCode()).isEqualTo(200);inputs.add(response.body());
                        assertThat(response.body().readNBytes(10)).isNotEmpty();
                    }
                    gateway.streams.forEach(stream->stream.release.countDown());
                    for(var input:inputs)assertThat(new String(input.readAllBytes(),StandardCharsets.UTF_8)).contains("event:completed");
                }finally {gateway.streams.forEach(stream->stream.release.countDown());for(var input:inputs)input.close();}
            }
        }
    }
}
