package kr.douid.brand.work.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.infrastructure.persistence.CategoryJpaRepository;
import kr.douid.brand.media.domain.Media;
import kr.douid.brand.media.infrastructure.persistence.MediaJpaRepository;
import kr.douid.brand.shared.config.JpaConfig;
import kr.douid.brand.work.application.query.AdminWorkDetail;
import kr.douid.brand.work.application.query.AdminWorkListItem;
import kr.douid.brand.work.application.query.PublicWorkDetail;
import kr.douid.brand.work.application.query.PublicWorkListItem;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkMediaItem;
import kr.douid.brand.work.domain.WorkMediaRole;
import kr.douid.brand.work.domain.WorkVisibility;
import kr.douid.brand.work.infrastructure.persistence.WorkJpaRepository;
import kr.douid.brand.work.infrastructure.query.QuerydslWorkQueryRepositoryAdapter;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslWorkQueryRepositoryAdapter.class})
class QuerydslWorkQueryRepositoryAdapterTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private WorkJpaRepository workJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private MediaJpaRepository mediaJpaRepository;

    @Autowired
    private QuerydslWorkQueryRepositoryAdapter adapter;

    private Category saveCategory(String name, String slug, boolean visible) {
        return categoryJpaRepository.save(Category.create(name, slug, 0, visible));
    }

    private Media saveMedia(String filename) {
        return mediaJpaRepository.save(
                Media.upload(filename, filename + "-stored", "media/" + filename, "image/png", 100L));
    }

    private Work saveWork(String title, String slug, Long categoryId, WorkVisibility visibility) {
        return workJpaRepository.save(Work.create(title, slug, "요약", "상세", categoryId, visibility));
    }

    @Test
    void findAdminWorkList_공개_비공개_모두_조회() {
        Category category = saveCategory("브랜딩", "branding", false);
        saveWork("공개 작업물", "public-work", category.getId(), WorkVisibility.VISIBLE);
        saveWork("비공개 작업물", "hidden-work", category.getId(), WorkVisibility.HIDDEN);

        Page<AdminWorkListItem> result = adapter.findAdminWorkList(PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(AdminWorkListItem::visibility)
                .containsExactlyInAnyOrder(WorkVisibility.VISIBLE, WorkVisibility.HIDDEN);
    }

    @Test
    void findAdminWorkList_비공개_카테고리_소속_Work도_조회() {
        Category hiddenCategory = saveCategory("비공개 카테고리", "hidden-category", false);
        saveWork("작업물", "work-in-hidden-category", hiddenCategory.getId(), WorkVisibility.VISIBLE);

        Page<AdminWorkListItem> result = adapter.findAdminWorkList(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).category().visible()).isFalse();
    }

    @Test
    void findAdminWorkList_동일_생성시각_id_보조정렬_안정성() {
        Category category = saveCategory("브랜딩", "branding", true);
        Work first = saveWork("첫번째", "first-work", category.getId(), WorkVisibility.VISIBLE);
        Work second = saveWork("두번째", "second-work", category.getId(), WorkVisibility.VISIBLE);

        Page<AdminWorkListItem> result = adapter.findAdminWorkList(PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(AdminWorkListItem::id)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void findPublicWorkList_공개_공개_조합만_노출() {
        Category visibleCategory = saveCategory("공개 카테고리", "visible-category", true);
        Category hiddenCategory = saveCategory("비공개 카테고리", "hidden-category", false);

        saveWork("공개+공개", "visible-visible", visibleCategory.getId(), WorkVisibility.VISIBLE);
        saveWork("비공개+공개", "hidden-visible", visibleCategory.getId(), WorkVisibility.HIDDEN);
        saveWork("공개+비공개", "visible-hidden", hiddenCategory.getId(), WorkVisibility.VISIBLE);
        saveWork("비공개+비공개", "hidden-hidden", hiddenCategory.getId(), WorkVisibility.HIDDEN);
        saveWork("카테고리없음", "no-category", null, WorkVisibility.VISIBLE);

        Page<PublicWorkListItem> result = adapter.findPublicWorkList(PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(PublicWorkListItem::slug)
                .containsExactly("visible-visible");
    }

    @Test
    void findAdminWorkDetail_비공개_Work_비공개_Category_조회_성공_WorkMedia_조합() {
        Category hiddenCategory = saveCategory("비공개 카테고리", "hidden-category", false);
        Media thumbnailMedia = saveMedia("thumb.png");
        Media detailMedia = saveMedia("detail.png");

        Work work = saveWork("비공개 작업물", "hidden-detail-work", hiddenCategory.getId(), WorkVisibility.HIDDEN);
        work.replaceMediaItems(List.of(
                new WorkMediaItem(detailMedia.getId(), WorkMediaRole.DETAIL_IMAGE, 1, "상세"),
                new WorkMediaItem(thumbnailMedia.getId(), WorkMediaRole.THUMBNAIL, 0, "대표")));
        workJpaRepository.saveAndFlush(work);

        Optional<AdminWorkDetail> result = adapter.findAdminWorkDetail(work.getId());

        assertThat(result).isPresent();
        AdminWorkDetail detail = result.get();
        assertThat(detail.visibility()).isEqualTo(WorkVisibility.HIDDEN);
        assertThat(detail.category().visible()).isFalse();
        assertThat(detail.mediaItems())
                .extracting(item -> item.role())
                .containsExactly(WorkMediaRole.THUMBNAIL, WorkMediaRole.DETAIL_IMAGE);
        assertThat(detail.mediaItems().get(0).mediaId()).isEqualTo(thumbnailMedia.getId());
    }

    @Test
    void findAdminWorkDetail_미존재_empty() {
        Optional<AdminWorkDetail> result = adapter.findAdminWorkDetail(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicWorkDetailBySlug_공개_공개_조합만_성공() {
        Category visibleCategory = saveCategory("공개 카테고리", "visible-category", true);
        Media media = saveMedia("thumb.png");

        Work work = saveWork("공개 작업물", "public-detail-work", visibleCategory.getId(), WorkVisibility.VISIBLE);
        work.replaceMediaItems(List.of(new WorkMediaItem(media.getId(), WorkMediaRole.THUMBNAIL, 0, "대표 이미지")));
        workJpaRepository.saveAndFlush(work);

        Optional<PublicWorkDetail> result = adapter.findPublicWorkDetailBySlug("public-detail-work");

        assertThat(result).isPresent();
        assertThat(result.get().mediaItems().get(0).altText()).isEqualTo("대표 이미지");
    }

    @Test
    void findPublicWorkDetailBySlug_Work공개_Category비공개_empty() {
        Category hiddenCategory = saveCategory("비공개 카테고리", "hidden-category-detail", false);
        saveWork("공개 작업물", "visible-work-hidden-category", hiddenCategory.getId(), WorkVisibility.VISIBLE);

        Optional<PublicWorkDetail> result = adapter.findPublicWorkDetailBySlug("visible-work-hidden-category");

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicWorkListByCategorySlug_같은_공개_카테고리_공개_Work만_포함() {
        Category category = saveCategory("공개 카테고리", "same-category", true);
        saveWork("공개 작업물", "same-category-visible", category.getId(), WorkVisibility.VISIBLE);
        saveWork("비공개 작업물", "same-category-hidden", category.getId(), WorkVisibility.HIDDEN);

        Page<PublicWorkListItem> result = adapter.findPublicWorkListByCategorySlug(
                "same-category", PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(PublicWorkListItem::slug)
                .containsExactly("same-category-visible");
    }

    @Test
    void findPublicWorkListByCategorySlug_비공개_카테고리는_빈결과() {
        Category hiddenCategory = saveCategory("비공개 카테고리", "hidden-category-list", false);
        saveWork("작업물", "work-in-hidden-list-category", hiddenCategory.getId(), WorkVisibility.VISIBLE);

        Page<PublicWorkListItem> result = adapter.findPublicWorkListByCategorySlug(
                "hidden-category-list", PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findPublicWorkListByCategorySlug_존재하지않는_카테고리는_빈결과() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));

        Page<PublicWorkListItem> result = adapter.findPublicWorkListByCategorySlug("missing-slug", pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
