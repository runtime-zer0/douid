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

    record Question(String id, String text, boolean terms, List<AiChatCommand.HistoryMessage> history) {}

    static final List<Question> QUESTIONS = List.of(
            new Question("M1", "어떤 서비스 하세요? 종류별로 정리해 주세요", false, List.of()),
            new Question("M2", "브랜딩 의뢰하면 진행 과정이 어떻게 되나요? 단계별로 알려주세요", false, List.of()),
            new Question("M3", "카페 브랜딩이랑 패키지 작업 있으면 목록으로 보여주세요", true, List.of()),
            new Question("M4", "디자인 의뢰 전에 준비해야 할 것들을 항목별로 알려주세요", false, List.of()));

    static final List<AiChatCommand.HistoryMessage> LONG_PLAIN = longPlainHistory();
    // 수정 전 답변이 브라우저 대화 저장소에 남아 이력으로 다시 들어오는 경우
    static final List<AiChatCommand.HistoryMessage> MARKDOWN_HISTORY = List.of(
            user("카페 브랜딩 작업 있어요?"),
            assistant("네, 관련 작업이 있습니다.\n\n### 카페 브랜딩\n- **카페 멜로우 브랜딩**: 따뜻한 톤의 동네 카페 리브랜딩\n- **북카페 페이지 아이덴티티**: 로고와 사인"),
            user("의뢰하면 뭐부터 하나요?"),
            assistant("**진행 과정**은 아래와 같습니다.\n\n| 단계 | 내용 |\n| --- | --- |\n| 1 | 상담 |\n| 2 | 시안 |\n\n자세한 내용은 **카카오톡 채널**로 문의해 주세요."),
            user("고마워요"),
            assistant("## 감사합니다\n궁금한 점이 있으면 **언제든** 물어보세요."));

    static final List<Question> HISTORY_QUESTIONS = List.of(
            new Question("L1", "지금까지 얘기한 내용 항목별로 정리해 주세요", false, LONG_PLAIN),
            new Question("L2", "카페 브랜딩이랑 패키지 작업 다시 목록으로 보여주세요", true, LONG_PLAIN),
            new Question("D1", "의뢰 전에 준비할 것들 항목별로 알려주세요", false, MARKDOWN_HISTORY),
            new Question("D2", "카페 브랜딩이랑 패키지 작업 다시 목록으로 보여주세요", true, MARKDOWN_HISTORY));

    @Test
    void 목록형_질문_답변에_마크다운_기호가_있는지_출력한다() throws Exception {
        evaluate(QUESTIONS);
    }

    @Test
    void 긴_이력과_마크다운_이력에서도_목록형_답변에_기호가_있는지_출력한다() throws Exception {
        evaluate(HISTORY_QUESTIONS);
    }

    // #79: 긴 이력 뒤 재요청에서 검색 결과 대신 이력 안내만 답하는 빈도 확인
    @Test
    void 긴_이력_뒤_작업물_재요청이_검색결과로_답하는지_출력한다() throws Exception {
        evaluate(List.of(HISTORY_QUESTIONS.get(1)), 8);
    }

    private void evaluate(List<Question> questions) throws Exception {
        evaluate(questions, REPEATS);
    }

    private void evaluate(List<Question> questions, int repeats) throws Exception {
        var properties = new OpenAiProperties();
        properties.setEnabled(true);
        properties.setApiKey(System.getenv("OPENAI_API_KEY"));
        int markdownAnswers = 0;
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
             var executor = new BoundedChatStreamExecutor(1)) {
            var metrics = new SimpleMeterRegistry();
            var gateway = new OpenAiResponsesGateway(properties, client, executor, metrics);
            for (var question : questions) {
                var resolver = new PortfolioContextResolver((text, limit) -> WORKS, text -> question.terms());
                for (int repeat = 1; repeat <= repeats; repeat++) {
                    var context = resolver.resolve(question.text());
                    var answer = new StringBuilder();
                    try (var source = gateway.prepare(new AiChatCommand(question.text(), question.history()))) {
                        source.run(context, answer::append);
                    }
                    String text = answer.toString();
                    var matcher = MARKDOWN.matcher(text);
                    boolean markdown = matcher.find();
                    if (markdown) markdownAnswers++;
                    long mentioned = WORKS.stream().filter(work -> text.contains(work.title())).count();
                    System.out.printf("EVAL %s#%d history=%d mentioned=%d/%d markdown=%s%s%n----%n%s%n====%n", question.id(), repeat,
                            question.history().size(), mentioned, WORKS.size(), markdown,
                            markdown ? " first=" + matcher.group().strip() : "", text);
                }
            }
            var attempts = metrics.find("ai.provider.attempts").counters().stream()
                    .mapToLong(c -> (long) c.count()).sum();
            System.out.println("EVAL calls=" + attempts + " markdownAnswers=" + markdownAnswers);
            assertThat(attempts).isLessThanOrEqualTo(16L);
        }
    }

    static List<AiChatCommand.HistoryMessage> longPlainHistory() {
        String[][] turns = {
                {"안녕하세요, 작은 카페를 준비하고 있어요", "안녕하세요. 카페 준비 중이시군요. 어떤 디자인이 필요하신지 알려주시면 안내해 드릴게요."},
                {"로고랑 간판이 필요해요", "로고와 간판은 브랜딩 작업으로 함께 진행하는 경우가 많습니다. 구체적인 범위는 상담이 필요합니다."},
                {"비용은 대략 얼마예요?", "가격은 여기서 확정해 드릴 수 없습니다. 견적은 DOUID 카카오톡 채널로 문의해 주세요."},
                {"기간은 얼마나 걸려요?", "제작 기간도 작업 범위에 따라 달라져 확정해 드리기 어렵습니다. 일정은 추가 상담이 필요합니다."},
                {"분위기는 따뜻한 느낌이면 좋겠어요", "따뜻한 분위기를 원하신다는 점은 상담할 때 함께 전달해 주시면 좋습니다."},
                {"메뉴판 디자인도 되나요?", "메뉴판처럼 매장에서 쓰는 인쇄물도 문의하실 수 있습니다. 가능 여부와 범위는 상담에서 확인해 주세요."},
                {"패키지도 생각 중이에요", "원두나 디저트 포장을 생각하시면 패키지 디자인도 함께 상담해 보실 수 있습니다."},
                {"참고할 만한 자료는 뭘 준비하죠?", "원하는 분위기의 사진, 기존 자료, 매장 위치와 주요 고객층을 정리해 두시면 도움이 됩니다."},
                {"인스타그램용 이미지도 필요할까요?", "온라인 홍보 계획이 있으시면 필요한 결과물 목록에 함께 적어 두시는 것이 좋습니다."},
                {"알겠어요, 조금 더 고민해볼게요", "네, 천천히 정리하시고 궁금한 점이 생기면 다시 물어봐 주세요."}};
        var history = new java.util.ArrayList<AiChatCommand.HistoryMessage>();
        for (var turn : turns) {
            history.add(user(turn[0]));
            history.add(assistant(turn[1]));
        }
        return List.copyOf(history);
    }

    static AiChatCommand.HistoryMessage user(String content) {
        return new AiChatCommand.HistoryMessage("user", content);
    }

    static AiChatCommand.HistoryMessage assistant(String content) {
        return new AiChatCommand.HistoryMessage("assistant", content);
    }
}
