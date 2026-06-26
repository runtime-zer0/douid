package kr.douid.brand.work.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * 작업물 Aggregate Root
 *
 * WorkMedia는 이 Aggregate를 통해서만 생성·변경·삭제된다.
 */
@Getter
@Entity
@Table(name = "works")
public class Work extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkVisibility visibility;

    @OneToMany(mappedBy = "work", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkMedia> mediaItems = new ArrayList<>();

    protected Work() {
    }

    private Work(String title, String slug, String summary, String description,
            Long categoryId, WorkVisibility visibility) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.description = description;
        this.categoryId = categoryId;
        this.visibility = visibility;
    }

    /**
     * 새 작업물 인스턴스를 생성
     *
     * @param title       작업물 제목
     * @param slug        작업물 슬러그
     * @param summary     요약 설명
     * @param description 상세 설명
     * @param categoryId  연결할 카테고리 ID (없으면 null)
     * @param visibility  공개 여부
     * @return 생성된 작업물
     */
    public static Work create(String title, String slug, String summary, String description,
            Long categoryId, WorkVisibility visibility) {
        return new Work(title, slug, summary, description, categoryId, visibility);
    }

    /**
     * 작업물 기본 정보를 변경
     *
     * @param title       작업물 제목
     * @param slug        작업물 슬러그
     * @param summary     요약 설명
     * @param description 상세 설명
     * @param categoryId  연결할 카테고리 ID (없으면 null)
     */
    public void updateBasicInfo(String title, String slug, String summary, String description,
            Long categoryId) {
        this.title = title;
        this.slug = slug;
        this.summary = summary;
        this.description = description;
        this.categoryId = categoryId;
    }

    /**
     * 공개 여부를 변경
     *
     * @param visibility 변경할 공개 여부
     */
    public void changeVisibility(WorkVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * WorkMedia 목록을 새 구성으로 전체 교체
     *
     * THUMBNAIL 역할은 최대 하나만 허용한다.
     *
     * @param items 교체할 미디어 항목 목록
     * @throws WorkThumbnailDuplicateException THUMBNAIL 역할이 2개 이상인 경우
     */
    public void replaceMediaItems(List<WorkMediaItem> items) {
        long thumbnailCount = items.stream()
                .filter(item -> WorkMediaRole.THUMBNAIL == item.role())
                .count();
        if (thumbnailCount > 1) {
            throw new WorkThumbnailDuplicateException();
        }
        this.mediaItems.clear();
        items.forEach(item -> this.mediaItems.add(
                new WorkMedia(this, item.mediaId(), item.role(), item.sortOrder(), item.altText())));
    }

}
