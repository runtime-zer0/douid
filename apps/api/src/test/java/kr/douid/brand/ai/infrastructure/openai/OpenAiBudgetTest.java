package kr.douid.brand.ai.infrastructure.openai;

import kr.douid.brand.ai.application.PortfolioContext;

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
            var useCase=new kr.douid.brand.ai.application.AiChatUseCase(gateway(),guard,policy,executor,new kr.douid.brand.ai.application.PortfolioContextResolver((t,l)->java.util.List.of(),t->false));
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
        try (var source = source()) { source.run(PortfolioContext.noTerms(),s -> {}); }
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
            assertThatThrownBy(() -> source.run(PortfolioContext.noTerms(),s -> {})).isInstanceOf(LlmIntegrationException.class);
        }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{broken}", "{\"object\":\"other\",\"input_tokens\":1}"})
    void 손상된_Count_결과는_생성하지않음(String body) throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json", body);
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(PortfolioContext.noTerms(),s -> {})).isInstanceOf(LlmIntegrationException.class);
        }
        assertThat(stub.generationRequests).isEmpty();
    }
    @Test void Count_본문상한초과는_생성하지않음() throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json", " ".repeat(4097));
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(PortfolioContext.noTerms(),s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(LlmIntegrationException.Kind.PROVIDER_RESOURCE_LIMIT));
        }
        assertThat(stub.generationRequests).isEmpty();
    }
    @Test void prepare_이후의_설정변경으로_요청snapshot이_변경되지않음() throws Exception {
        try (var source = source()) {
            properties.setModel("client-fake"); properties.setMaxOutputTokens(999999);
            source.run(PortfolioContext.noTerms(),s -> {});
        }
        var create = OpenAiEventReader.JSON.readTree(stub.generationRequests.getFirst());
        assertThat(create.path("model").asString()).isEqualTo("gpt-6-luna");
        assertThat(create.path("max_output_tokens").asInt()).isEqualTo(1024);
    }
    static PortfolioContext worstCaseContext() {
        var items=new java.util.ArrayList<kr.douid.brand.work.application.port.PublicWorkSearchItem>();
        for(int i=0;i<5;i++) items.add(new kr.douid.brand.work.application.port.PublicWorkSearchItem(
                i+"가".repeat(254),"나".repeat(80),"다".repeat(120),"라".repeat(50),"마".repeat(100)));
        return new kr.douid.brand.ai.application.PortfolioContextResolver((t,l)->items,t->true).resolve("브랜딩");
    }
    @Test void 최악_검색결과_블록과_최대_클라이언트본문도_입력예산_근거안에_들어감() {
        var context=worstCaseContext();
        var composer=new OpenAiRequestComposer(properties);
        var history=new java.util.ArrayList<kr.douid.brand.ai.application.AiChatCommand.HistoryMessage>();
        for(int i=0;i<10;i++) history.add(new kr.douid.brand.ai.application.AiChatCommand.HistoryMessage(
                i%2==0?"user":"assistant","한".repeat(530)));
        var requests=composer.compose(new kr.douid.brand.ai.application.AiChatCommand("질".repeat(30),history),context.block());
        String instructions=OpenAiEventReader.JSON.readTree(requests.generation()).path("instructions").asString();
        int instructionCodePoints=instructions.codePointCount(0,instructions.length());
        int clientBytes=history.stream().mapToInt(h->h.content().getBytes(java.nio.charset.StandardCharsets.UTF_8).length).sum();
        System.out.printf("budget instructions+block=%d cp, block=%d cp, clientContent=%d B, count=%d B, generation=%d B%n",
                instructionCodePoints,context.block().codePointCount(0,context.block().length()),clientBytes,
                requests.count().length,requests.generation().length);
        assertThat(instructionCodePoints).isLessThanOrEqualTo(4446);
        assertThat(clientBytes).isGreaterThanOrEqualTo(15900);
        assertThat(requests.generation().length).isLessThan(32768);
    }
    @Test void 검색결과를_넣은_Count가_상한을_넘으면_생성하지않음() throws Exception {
        stub.count = e -> OpenAiStubServer.reply(e, 200, "application/json",
                "{\"object\":\"response.input_tokens\",\"input_tokens\":8193}");
        var context=worstCaseContext();
        try (var source = source()) {
            assertThatThrownBy(() -> source.run(context,s -> {})).isInstanceOfSatisfying(LlmIntegrationException.class,
                    e -> assertThat(e.kind()).isEqualTo(LlmIntegrationException.Kind.INPUT_BUDGET_EXCEEDED));
        }
        assertThat(stub.countRequests).hasSize(1); assertThat(stub.generationRequests).isEmpty();
        assertThat(OpenAiEventReader.JSON.readTree(stub.countRequests.getFirst()).path("instructions").asString())
                .endsWith(context.block());
    }
}
