package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;

class OpenAiMetricsTest extends OpenAiTestSupport {
    @Test void Count와_생성_Usage는_분리하고_종료당_한번만_집계() throws Exception {
        try (var source = source()) { source.run(s -> {}); source.close(); source.cancel(); }
        assertThat(metrics.get("ai.provider.attempts").tag("stage", "count").counter().count()).isEqualTo(1);
        assertThat(metrics.get("ai.provider.results").tags("stage", "count", "outcome", "success").counter().count()).isEqualTo(1);
        assertThat(metrics.get("ai.provider.attempts").tag("stage", "generation").counter().count()).isEqualTo(1);
        assertThat(metrics.get("ai.provider.tokens").tags("kind", "input", "outcome", "success").summary().totalAmount()).isEqualTo(100);
        assertThat(metrics.get("ai.provider.usage").tags("known", "true", "outcome", "success").counter().count()).isEqualTo(1);
        assertThat(metrics.getMeters()).allSatisfy(m -> assertThat(m.getId().getTags().toString())
                .doesNotContain("CANARY", "r1", "m1", "http"));
    }
    @Test void Usage가_없으면_0Token으로_집계하지않음() throws Exception {
        String body = OpenAiStubServer.success("hello").replace(
                ",\"usage\":{\"input_tokens\":100,\"output_tokens\":2,\"total_tokens\":102}", "");
        stub.generation = e -> OpenAiStubServer.reply(e, 200, "text/event-stream", body);
        try (var source = source()) { source.run(s -> {}); }
        assertThat(metrics.find("ai.provider.tokens").summary()).isNull();
        assertThat(metrics.get("ai.provider.usage").tags("known", "false", "outcome", "success").counter().count()).isEqualTo(1);
    }
    @Test void 불완전한_응답의_확인된_Usage도_실패outcome으로_집계() throws Exception {
        String body = OpenAiStubServer.success("partial").replace("response.completed", "response.incomplete")
                .replace("\"status\":\"completed\"", "\"status\":\"incomplete\"");
        stub.generation = e -> OpenAiStubServer.reply(e, 200, "text/event-stream", body);
        try (var source = source()) { assertThatThrownBy(() -> source.run(s -> {})).isInstanceOf(RuntimeException.class); }
        assertThat(metrics.get("ai.provider.usage").tags("known", "true", "outcome", "incomplete_other").counter().count()).isEqualTo(1);
    }
}
