package kr.douid.brand.ai.presentation;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import kr.douid.brand.ai.application.PortfolioContext;
import kr.douid.brand.ai.testsupport.AiChatTestConfiguration;
import kr.douid.brand.ai.testsupport.ControlledLlmGateway;
import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.ai-chat.rate-limit.requests=200", "app.ai-chat.max-concurrent-streams=1"})
@Import(AiChatTestConfiguration.class)
class AiChatRecommendationIntegrationTest extends PostgresIntegrationTest {
    @LocalServerPort int port;
    @Autowired ControlledLlmGateway gateway;
    @Autowired WorkJpaRepository works;
    @Autowired CategoryJpaRepository categories;
    @Autowired MeterRegistry meters;

    @BeforeEach
    void saveWorks() {
        gateway.last = null;
        if (works.count() > 0) return;
        var branding = categories.save(Category.create("Branding", "branding", 0, true));
        var hiddenCategory = categories.save(Category.create("Private", "private-cat", 1, false));
        works.saveAndFlush(Work.create("카페 멜로우 브랜딩", "cafe-mellow-branding", "동네 카페 리브랜딩", null,
                branding.getId(), WorkVisibility.VISIBLE));
        works.saveAndFlush(Work.create("카페 시크릿 브랜딩", "hidden-cafe-branding", "비공개", null,
                branding.getId(), WorkVisibility.HIDDEN));
        works.saveAndFlush(Work.create("카페 프라이빗 브랜딩", "private-category-cafe", "비공개 카테고리", null,
                hiddenCategory.getId(), WorkVisibility.VISIBLE));
    }

    @AfterEach
    void release() {
        gateway.streams.forEach(stream -> stream.release.countDown());
    }

    @Test
    @Timeout(15)
    void 공개_작업물만_맥락과_추천에_싣고_이력의_비공개_slug는_무시한다() throws Exception {
        // given
        gateway.behavior = (stream, delta) -> delta.accept("hidden-cafe-branding 작업도 있어요");
        String body = "{\"message\":\"카페 브랜딩 작업 있어요?\",\"history\":[{\"role\":\"assistant\","
                + "\"content\":\"hidden-cafe-branding, private-category-cafe 를 소개했어요\"}]}";

        // when
        String sse = post(body).body();

        // then
        var context = gateway.last.context;
        assertThat(context.state()).isEqualTo(PortfolioContext.State.FOUND);
        assertThat(context.block()).contains("cafe-mellow-branding")
                .doesNotContain("hidden-cafe-branding", "private-category-cafe", "카페 시크릿", "카페 프라이빗");
        assertThat(context.portfolioIds()).containsExactly("cafe-mellow-branding");
        assertThat(sse).containsSubsequence("event:message_delta", "event:recommendations",
                        "{\"portfolioIds\":[\"cafe-mellow-branding\"]}", "event:completed")
                .containsOnlyOnce("event:recommendations");
    }

    @Test
    @Timeout(15)
    void 동시실행_제한에_걸린_요청은_검색하지_않는다() throws Exception {
        // given
        gateway.behavior = (stream, delta) -> stream.awaitRelease();
        try (var client = HttpClient.newHttpClient()) {
            var holding = client.sendAsync(request("{\"message\":\"카페 브랜딩\"}"), HttpResponse.BodyHandlers.ofString());
            while (gateway.last == null || !gateway.last.entered.await(50, TimeUnit.MILLISECONDS)) Thread.onSpinWait();
            var first = gateway.last;
            long searchesBefore = searches();

            // when
            var limited = client.send(request("{\"message\":\"카페 브랜딩\"}"), HttpResponse.BodyHandlers.ofString());

            // then
            assertThat(limited.statusCode()).isEqualTo(429);
            assertThat(searches()).isEqualTo(searchesBefore);
            first.release.countDown();
            assertThat(holding.get(10, TimeUnit.SECONDS).body()).contains("event:completed");
        }
    }

    private long searches() {
        return meters.find("douid.work.search").timers().stream().mapToLong(Timer::count).sum();
    }

    private HttpResponse<String> post(String body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request(body), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            return response;
        }
    }

    private HttpRequest request(String body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/public/ai-chat/stream"))
                .header("Content-Type", "application/json").header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
}
