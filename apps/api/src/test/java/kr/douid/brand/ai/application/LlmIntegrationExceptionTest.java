package kr.douid.brand.ai.application;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import kr.douid.brand.shared.exception.IntegrationException;

class LlmIntegrationExceptionTest {
    @Test void 모든_분류는_원문없는_고정된_오류() {
        for (var kind : LlmIntegrationException.Kind.values()) {
            var failure = new LlmIntegrationException(kind);
            assertThat(failure).isInstanceOf(IntegrationException.class).hasNoCause();
            assertThat(failure.getMessage()).isEqualTo("AI provider request failed");
            assertThat(failure.kind()).isEqualTo(kind);
        }
    }
}
