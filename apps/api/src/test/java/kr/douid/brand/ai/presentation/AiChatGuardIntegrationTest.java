package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "app.security.allowed-origins=https://allowed.example"})
@Import({AiChatTestConfiguration.class,AiChatGuardIntegrationTest.HeaderProbe.class})
class AiChatGuardIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired ControlledLlmGateway gateway;
    HttpRequest request(String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                .header("Content-Type","application/json").header("Accept","text/event-stream")
                .timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
    @Test @Timeout(20)
    void 실제_8개_연결_상한과_요청실패_집계_재디스패치_미중복() throws Exception {
        gateway.behavior=(s,d)->{d.accept("open");s.awaitRelease();};
        List<InputStream> opened=new ArrayList<>();
        try(var client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
          try {
            for(int i=0;i<8;i++) {
                var response=client.send(request("{\"message\":\"hi\"}"),HttpResponse.BodyHandlers.ofInputStream());
                assertThat(response.statusCode()).isEqualTo(200);opened.add(response.body());
                assertThat(response.body().readNBytes(10)).isNotEmpty();
            }
            var busy=client.send(request("{\"message\":\"hi\"}"),HttpResponse.BodyHandlers.ofString());
            assertThat(busy.statusCode()).isEqualTo(429);assertThat(busy.body()).contains("AI_BUSY");
            assertThat(busy.headers().firstValue("Retry-After")).contains("1");
            gateway.streams.forEach(s->s.release.countDown());
            for(var input:opened){assertThat(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).contains("event:completed");input.close();}
            // 정상 8회와 busy 1회도 소비되며 ASYNC 종료는 중복 집계하지 않음
            for(int i=0;i<21;i++) {
                var invalid=client.send(request("{}"),HttpResponse.BodyHandlers.ofString());
                assertThat(invalid.statusCode()).isEqualTo(400);
            }
            var limited=client.send(request("{}"),HttpResponse.BodyHandlers.ofString());
            assertThat(limited.statusCode()).isEqualTo(429);assertThat(limited.body()).contains("AI_RATE_LIMITED");
            assertThat(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L,60L);
          } finally {gateway.streams.forEach(s->s.release.countDown());for(var input:opened)input.close();}
        }
    }
    @Test void 허용_Origin의_preflight와_비허용_Origin_차단() throws Exception {
        try(var client=HttpClient.newHttpClient()) {
            var uri=URI.create("http://localhost:"+port+"/api/public/ai-chat/stream");
            var preflight=HttpRequest.newBuilder(uri).header("Origin","https://allowed.example")
                    .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type")
                    .method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
            var allowed=client.send(preflight,HttpResponse.BodyHandlers.ofString());
            assertThat(allowed.statusCode()).isEqualTo(200);
            assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).contains("https://allowed.example");
            var denied=HttpRequest.newBuilder(uri).header("Origin","https://denied.example")
                    .GET().build();
            assertThat(client.send(denied,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        }
    }
    @Test void 보안헤더는_비동기_전송전_이미_작성되어야_함() throws Exception {
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/header-probe"))
                    .GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.body()).isEqualTo("nosniff");
            assertThat(response.headers().firstValue("X-Frame-Options")).contains("DENY");
        }
    }
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class HeaderProbe {
        @org.springframework.context.annotation.Bean ProbeController headerProbeController(){return new ProbeController();}
    }
    @org.springframework.web.bind.annotation.RestController
    static class ProbeController {
        @org.springframework.web.bind.annotation.GetMapping("/api/public/header-probe")
        String probe(jakarta.servlet.http.HttpServletResponse response) {
            String value=response.getHeader("X-Content-Type-Options");return value==null?"missing":value;
        }
    }
}
