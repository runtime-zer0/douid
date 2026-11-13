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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.*;
import kr.douid.brand.ai.testsupport.*;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "app.ai-chat.execution-timeout=300ms","app.ai-chat.emitter-timeout=1000ms","app.ai-chat.rate-limit.requests=200"})
@Import({AiChatTestConfiguration.class,AiChatLifecycleIntegrationTest.DelayedActivation.class})
@Timeout(15)
class AiChatLifecycleIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired ControlledLlmGateway gateway;
    static volatile boolean delayActivation;
    HttpRequest request() {
        return HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/public/ai-chat/stream"))
                .header("Content-Type","application/json").header("Accept","text/event-stream")
                .timeout(Duration.ofSeconds(5)).POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"hi\"}")).build();
    }
    @AfterEach void release(){delayActivation=false;if(gateway.last!=null)gateway.last.release.countDown();}
    @Test void 초기화전_deadline은_첫쓰기없이_504_JSON_소스미실행() throws Exception {
        delayActivation=true;gateway.behavior=(s,d)->{throw new AssertionError("must not run");};
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(504);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
            assertThat(response.body()).contains("AI_TIMEOUT").doesNotContain("event:");
            assertThat(gateway.last.entered.getCount()).isEqualTo(1);
        }
    }
    @Test void 부분_Delta후_예외는_안전한_error만_전달() throws Exception {
        gateway.behavior=(s,d)->{d.accept("partial");throw new RuntimeException("secret-provider-canary");};
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).containsSubsequence("event:started","event:message_delta","event:error");
            assertThat(response.body()).contains("STREAM_FAILED").doesNotContain("event:completed","secret-provider-canary","\"status\":\"ERROR\"");
        }
    }
    @Test void 실행_timeout은_completed없이_STREAM_TIMEOUT() throws Exception {
        gateway.behavior=(s,d)->{d.accept("partial");s.awaitRelease();};
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.body()).contains("STREAM_TIMEOUT").doesNotContain("event:completed");
            assertThat(gateway.last.cancelled.get()).isTrue();
            assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
        }
    }
    @Test void 취소무시_source의_container_timeout은_추가JSON없이_연결종료() throws Exception {
        gateway.behavior=(s,d)->{d.accept("partial");while(s.release.getCount()>0) {
            try{s.awaitRelease();}catch(InterruptedException ignored){}
        }};
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.body()).contains("event:started","partial")
                    .doesNotContain("event:completed","\"status\":\"ERROR\"","INTERNAL_ERROR");
            assertThat(gateway.last.cancelled.get()).isTrue();
            assertThat(gateway.last.closed.getCount()).isEqualTo(1);
        }
    }
    @Test void client_close후_후속쓰기_실패를_감지하고_source_정리() throws Exception {
        gateway.behavior=(s,d)->{d.accept("first");s.awaitRelease();
            for(int i=0;i<100;i++)d.accept("x".repeat(2048));};
        try(var client=HttpClient.newHttpClient()) {
            var response=client.send(request(),HttpResponse.BodyHandlers.ofInputStream());
            try(var input=response.body()){assertThat(input.readNBytes(20)).isNotEmpty();}
            gateway.last.release.countDown();
            assertThat(gateway.last.closed.await(3,TimeUnit.SECONDS)).isTrue();
            assertThat(gateway.last.cancelled.get()).isTrue();
        }
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class DelayedActivation implements WebMvcConfigurer {
        @Autowired ControlledLlmGateway gateway;
        @Override public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new AsyncHandlerInterceptor() {
                @Override public void afterConcurrentHandlingStarted(HttpServletRequest request,HttpServletResponse response,Object handler) {
                    if(delayActivation) {
                        try {
                            if(!gateway.last.cancelRequested.await(3,TimeUnit.SECONDS)) {
                                throw new IllegalStateException("test deadline did not fire");
                            }
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("test activation interrupted");
                        }
                    }
                }
            }).addPathPatterns("/api/public/ai-chat/stream").order(100);
        }
    }
}
