package kr.douid.brand.ai.testsupport;
import java.time.Duration;
import kr.douid.brand.ai.application.AiChatPolicy;
public final class Policies {
    private Policies() {}
    /** Phase 07 기본 정책
     * @return 테스트 기본 제한
     */
    public static AiChatPolicy defaults() {
        return new AiChatPolicy(16384, 2000, 12, 2000, 12000, 30, Duration.ofSeconds(60), 8,
                Duration.ofSeconds(60), Duration.ofSeconds(65), 65536, 4096);
    }
}
