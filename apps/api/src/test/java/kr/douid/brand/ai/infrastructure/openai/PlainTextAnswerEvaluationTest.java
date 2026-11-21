package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;

import java.net.http.HttpClient;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kr.douid.brand.ai.application.AiChatCommand;
import kr.douid.brand.ai.application.PortfolioContextResolver;
import kr.douid.brand.ai.infrastructure.config.OpenAiProperties;
import kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor;
import kr.douid.brand.work.application.port.PublicWorkSearchItem;

/**
 * 목록형 질문 답변의 마크다운 기호 실제 Provider 평가, 사용자 승인 후 1회만 실행
 */
@EnabledIfEnvironmentVariable(named = "DOUID_PROVIDER_EVAL", matches = "true")
class PlainTextAnswerEvaluationTest {
    static final int REPEATS = 2;
    static final Pattern MARKDOWN = Pattern.compile("\\*\\*|__|(?m)^\\s*#{1,6}\\s|(?m)^\\s*\\|.*\\||```|(?m)^\\s*\\* ");
    static final List<PublicWorkSearchItem> WORKS = List.of(
            new PublicWorkSearchItem("cafe-mellow-branding", "카페 멜로우 브랜딩", "따뜻한 톤의 동네 카페 리브랜딩", "Branding", "branding"),
            new PublicWorkSearchItem("bookcafe-page-identity", "북카페 페이지 아이덴티티", "책과 커피를 함께 파는 공간의 로고와 사인", "Branding", "branding"),
            new PublicWorkSearchItem("sunny-jam-package", "써니잼 패키지", "수제 잼 병 라벨과 선물 상자", "Packaging Design", "packaging"));

    record Question(String id, String text, boolean terms) {}

    static final List<Question> QUESTIONS = List.of(
            new Question("M1", "어떤 서비스 하세요? 종류별로 정리해 주세요", false),
            new Question("M2", "브랜딩 의뢰하면 진행 과정이 어떻게 되나요? 단계별로 알려주세요", false),
            new Question("M3", "카페 브랜딩이랑 패키지 작업 있으면 목록으로 보여주세요", true),
            new Question("M4", "디자인 의뢰 전에 준비해야 할 것들을 항목별로 알려주세요", false));

    @Test
    void 목록형_질문_답변에_마크다운_기호가_있는지_출력한다() throws Exception {
        var properties = new OpenAiProperties();
        properties.setEnabled(true);
        properties.setApiKey(System.getenv("OPENAI_API_KEY"));
        int markdownAnswers = 0;
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
             var executor = new BoundedChatStreamExecutor(1)) {
            var metrics = new SimpleMeterRegistry();
            var gateway = new OpenAiResponsesGateway(properties, client, executor, metrics);
            for (var question : QUESTIONS) {
                var resolver = new PortfolioContextResolver((text, limit) -> WORKS, text -> question.terms());
                for (int repeat = 1; repeat <= REPEATS; repeat++) {
                    var context = resolver.resolve(question.text());
                    var answer = new StringBuilder();
                    try (var source = gateway.prepare(new AiChatCommand(question.text(), List.of()))) {
                        source.run(context, answer::append);
                    }
                    String text = answer.toString();
                    var matcher = MARKDOWN.matcher(text);
                    boolean markdown = matcher.find();
                    if (markdown) markdownAnswers++;
                    System.out.printf("EVAL %s#%d markdown=%s%s%n----%n%s%n====%n", question.id(), repeat, markdown,
                            markdown ? " first=" + matcher.group().strip() : "", text);
                }
            }
            var attempts = metrics.find("ai.provider.attempts").counters().stream()
                    .mapToLong(c -> (long) c.count()).sum();
            System.out.println("EVAL calls=" + attempts + " markdownAnswers=" + markdownAnswers);
            assertThat(attempts).isLessThanOrEqualTo(16L);
        }
    }
}
