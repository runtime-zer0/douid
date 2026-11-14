package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.Test;
import kr.douid.brand.ai.application.LlmIntegrationException;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;
import static kr.douid.brand.ai.application.LlmIntegrationException.Kind.*;

class OpenAiProviderFailureTest extends OpenAiTestSupport {
    @ParameterizedTest @ValueSource(strings={"https://example.com/v1/responses","http://localhost:123/v1/responses",
            "http://127.0.0.1:123/wrong","http://user@127.0.0.1:123/v1/responses","http://127.0.0.1:123/v1/responses?redirect=yes"})
    void 테스트_주입_경계도_임의의_외부주소를_허용하지않음(String url) {
        assertThatThrownBy(()->OpenAiResponsesGateway.forLoopbackTest(properties,client,executor,metrics,java.net.URI.create(url)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void 연결_기한초과는_원문없는_중립_timeout() throws Exception {
        var transport=org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        org.mockito.Mockito.when(transport.followRedirects()).thenReturn(java.net.http.HttpClient.Redirect.NEVER);
        org.mockito.Mockito.when(transport.send(org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<java.io.InputStream>>any()))
                .thenThrow(new java.net.http.HttpConnectTimeoutException("PRIVATE_KEY_CANARY"));
        var adapter=OpenAiResponsesGateway.forLoopbackTest(properties,transport,executor,metrics,stub.endpoint());
        try(var source=adapter.prepare(new kr.douid.brand.ai.application.AiChatCommand("hello",java.util.List.of()))) {
            assertThatThrownBy(()->source.run(s->{})).isInstanceOfSatisfying(LlmIntegrationException.class,e->{
                assertThat(e.kind()).isEqualTo(CONNECT_TIMEOUT);assertThat(e.getCause()).isNull();
                assertThat(e.toString()).doesNotContain("CANARY");
            });
        }
    }
    @ParameterizedTest @CsvSource({"401,AUTHENTICATION", "403,AUTHENTICATION", "429,RATE_LIMIT",
            "500,UNAVAILABLE", "503,UNAVAILABLE", "400,PROVIDER_FAILED", "302,PROVIDER_FAILED"})
    void Count거부시_생성없이_안전한_실패(int status, String kind) throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, status, "application/json", "RAW_ERROR_KEY_CANARY");
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class, e -> {
                assertThat(e.kind().name()).isEqualTo(kind);
                assertThat(e.getMessage()).doesNotContain("CANARY"); assertThat(e.getCause()).isNull();
            });
        }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).isEmpty();
    }
    @ParameterizedTest @ValueSource(ints = {401, 429, 500})
    void 생성거부도_재시도없이_한번(int status) throws Exception {
        stub.generation = e -> OpenAiStubServer.reply(e, status, "application/json", "PRIVATE_ERROR");
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOf(LlmIntegrationException.class);
        }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).hasSize(1);
    }
    @Test void 부분응답_후_불완전은_정상반환하지않음() throws Exception {
        String incomplete = OpenAiStubServer.success("partial").replace("response.completed", "response.incomplete")
                .replace("\"status\":\"completed\"", "\"status\":\"incomplete\"");
        stub.generation = e -> OpenAiStubServer.reply(e, 200, "text/event-stream", incomplete);
        var deltas = new ArrayList<String>();
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(deltas::add)).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(INCOMPLETE_OTHER));
        }
        assertThat(deltas).containsExactly("partial"); assertThat(stub.generationRequests).hasSize(1);
    }
    @Test void 잘못된_콘텐츠유형은_본문노출없이_실패() throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "text/html", "BODY_CANARY");
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(PROTOCOL));
        }
        assertThat(stub.generationRequests).isEmpty();
    }
}
