package kr.douid.brand.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

@Getter
@Entity
@Table(name = "categories")
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean visible;

    protected Category() {
    }

    private Category(String name, String slug, int displayOrder, boolean visible) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
        this.visible = visible;
    }

    /**
     * 새 카테고리 인스턴스를 생성
     *
     * @param name 카테고리 이름
     * @param slug 카테고리 slug
     * @param displayOrder 카테고리 노출 순서
     * @param visible 카테고리 공개 여부
     * @return 생성된 카테고리
     */
    public static Category create(String name, String slug, int displayOrder, boolean visible) {
        return new Category(name, slug, displayOrder, visible);
    }

    /**
     * 카테고리 정보를 변경
     *
     * @param name 카테고리 이름
     * @param slug 카테고리 slug
     * @param displayOrder 카테고리 노출 순서
     * @param visible 카테고리 공개 여부
     */
    public void update(String name, String slug, int displayOrder, boolean visible) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
        this.visible = visible;
    }
}
