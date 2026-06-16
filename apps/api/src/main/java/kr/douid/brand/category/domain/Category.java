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

    public static Category create(String name, String slug, int displayOrder, boolean visible) {
        return new Category(name, slug, displayOrder, visible);
    }

    public void update(String name, String slug, int displayOrder, boolean visible) {
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
        this.visible = visible;
    }
}
