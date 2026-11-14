package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import kr.douid.brand.ai.application.LlmIntegrationException;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;

class OpenAiBudgetTest extends OpenAiTestSupport {
    @Test void 로컬_동시제한과_빈도제한은_Count도_시작하지않음() {
        var policy=kr.douid.brand.ai.testsupport.Policies.defaults();
        var guard=new kr.douid.brand.ai.infrastructure.guard.InMemoryChatRequestGuard(policy,()->0L);
        try(var one=guard.acquire();var two=guard.acquire()) {
            var useCase=new kr.douid.brand.ai.application.AiChatUseCase(gateway(),guard,policy,executor);
            assertThatThrownBy(()->useCase.prepare(new kr.douid.brand.ai.application.AiChatCommand("hello",java.util.List.of())))
                    .isInstanceOf(kr.douid.brand.ai.application.AiChatException.class);
        }
        for(int i=0;i<10;i++)guard.attempt();
        assertThatThrownBy(guard::attempt).isInstanceOf(kr.douid.brand.ai.application.AiChatException.class);
        assertThat(stub.countRequests).isEmpty();assertThat(stub.generationRequests).isEmpty();
    }
    @ParameterizedTest @ValueSource(ints = {0, 8192})
    void 입력한도_경계까지_생성_한번(int tokens) throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json",
                "{\"object\":\"response.input_tokens\",\"input_tokens\":"+tokens+"}");
        try (var source = source()) { source.run(s -> {}); }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).hasSize(1);
        var count = OpenAiEventReader.JSON.readTree(stub.countRequests.getFirst());
        var create = OpenAiEventReader.JSON.readTree(stub.generationRequests.getFirst());
        assertThat(count.path("input")).isEqualTo(create.path("input"));
        assertThat(count.path("instructions")).isEqualTo(create.path("instructions"));
        assertThat(create.path("max_output_tokens").asInt()).isEqualTo(1024);
    }
    @ParameterizedTest @ValueSource(strings = {"8193", "-1", "0.5", "1e0", "\"1\"", "null", "9223372036854775808"})
    void 무효한_Count_또는_초과는_생성하지않음(String tokens) throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json",
                "{\"object\":\"response.input_tokens\",\"input_tokens\":"+tokens+"}");
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOf(LlmIntegrationException.class);
        }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{broken}", "{\"object\":\"other\",\"input_tokens\":1}"})
    void 손상된_Count_결과는_생성하지않음(String body) throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json", body);
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOf(LlmIntegrationException.class);
        }
        assertThat(stub.generationRequests).isEmpty();
    }
    @Test void Count_본문상한초과는_생성하지않음() throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json", " ".repeat(4097));
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(LlmIntegrationException.Kind.PROVIDER_RESOURCE_LIMIT));
        }
        assertThat(stub.generationRequests).isEmpty();
    }
    @Test void prepare_이후의_설정변경으로_요청snapshot이_변경되지않음() throws Exception {
        try (var source = source()) {
            properties.setModel("client-fake"); properties.setMaxOutputTokens(999999);
            source.run(s -> {});
        }
        var create = OpenAiEventReader.JSON.readTree(stub.generationRequests.getFirst());
        assertThat(create.path("model").asString()).isEqualTo("gpt-6-luna");
        assertThat(create.path("max_output_tokens").asInt()).isEqualTo(1024);
    }
}
