package kr.douid.brand.work.infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.shared.testsupport.PostgresIntegrationTest;
import kr.douid.brand.work.application.query.SearchTermGroup;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;
import kr.douid.brand.work.infrastructure.query.QuerydslPublicWorkSearchAdapter;

/**
 * 검색 지연 기록 전용 측정 (research R12)
 *
 * 장비마다 흔들리므로 단언하지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslPublicWorkSearchAdapter.class})
class PublicWorkSearchPerformanceTest extends PostgresIntegrationTest {

    private static final int WORKS = 200;
    private static final int RUNS = 20;

    @Autowired
    private QuerydslPublicWorkSearchAdapter adapter;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private WorkJpaRepository workJpaRepository;

    @Autowired
    private MediaJpaRepository mediaJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 공개_작업물_200건에서_최대_질의_지연을_기록한다() {
        Long categoryId = categoryJpaRepository.save(Category.create("Branding", "branding", 0, true)).getId();
        String description = "오래된 한옥의 결을 살린 브랜드 작업 설명 ".repeat(50).substring(0, 1_000);
        for (int i = 0; i < WORKS; i++) {
            Work work = Work.create("작업물 " + i + " 브랜딩", "work-" + i, "요약 " + i, description, categoryId,
                    WorkVisibility.VISIBLE);
            List<WorkMediaItem> items = new ArrayList<>();
            for (int m = 0; m < 3; m++) {
                Media media = mediaJpaRepository.save(Media.upload(
                        "m.png", "w" + i + "m" + m, "media/w" + i + "m" + m, "image/png", 1L));
                items.add(new WorkMediaItem(media.getId(), WorkMediaRole.DETAIL_IMAGE, m, "작업 사진 " + m));
            }
            work.replaceMediaItems(items);
            workJpaRepository.save(work);
        }
        entityManager.flush();
        entityManager.clear();

        List<SearchTermGroup> groups = List.of(
                new SearchTermGroup(List.of("카페", "커피", "cafe", "café", "coffee")),
                new SearchTermGroup(List.of("브랜딩", "브랜드", "brand", "리뉴얼", "renewal")),
                new SearchTermGroup(List.of("패키지", "패키징", "packag", "라벨", "label")),
                new SearchTermGroup(List.of("한옥", "스테이", "숙소", "stay", "hanok")),
                new SearchTermGroup(List.of("로고", "logo", "사인", "sign", "signage")),
                new SearchTermGroup(List.of("편집", "에디토리얼", "editorial", "굿즈", "goods")));

        measure("worst 6x5", groups);
        measure("typical 2 groups", List.of(
                new SearchTermGroup(List.of("카페", "커피", "cafe", "café", "coffee")),
                new SearchTermGroup(List.of("브랜딩", "브랜드", "brand"))));
    }

    private void measure(String label, List<SearchTermGroup> groups) {
        adapter.search(groups, 5);
        long[] millis = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            adapter.search(groups, 5);
            millis[i] = (System.nanoTime() - start) / 1_000_000;
        }
        Arrays.sort(millis);
        System.out.printf("work search latency [%s]: works=%d, runs=%d, median=%dms, max=%dms%n",
                label, WORKS, RUNS, (millis[RUNS / 2 - 1] + millis[RUNS / 2]) / 2, millis[RUNS - 1]);
    }
}
