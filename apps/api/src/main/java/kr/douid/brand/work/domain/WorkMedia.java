package kr.douid.brand.work.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Work Aggregate 내부 엔티티 — Work에서 Media가 사용되는 방식을 표현
 *
 * Media 엔티티를 직접 참조하지 않고 mediaId만 보유한다.
 * Work 도메인 메서드를 통해서만 생성·변경·삭제되어야 한다.
 */
@Getter
@Entity
@Table(name = "work_media")
public class WorkMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @Column(nullable = false)
    private Long mediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkMediaRole role;

    @Column(nullable = false)
    private int sortOrder;

    private String altText;

    protected WorkMedia() {
    }

    WorkMedia(Work work, Long mediaId, WorkMediaRole role, int sortOrder, String altText) {
        this.work = work;
        this.mediaId = mediaId;
        this.role = role;
        this.sortOrder = sortOrder;
        this.altText = altText;
    }
}
