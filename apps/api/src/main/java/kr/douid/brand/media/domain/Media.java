package kr.douid.brand.media.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

/**
 * 업로드된 파일 자산과 그 메타데이터를 나타내는 독립 리소스
 *
 * Work, Category 등 다른 Aggregate와 JPA 관계를 맺지 않는다.
 * 다른 도메인은 미디어 식별자(id)로만 참조
 */
@Getter
@Entity
@Table(name = "media")
public class Media extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storedFilename;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    protected Media() {
    }

    private Media(String originalFilename, String storedFilename, String filePath,
            String contentType, long fileSize) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    /**
     * 업로드된 파일로부터 미디어 인스턴스를 생성
     *
     * @param originalFilename 업로드 시 원본 파일명
     * @param storedFilename   충돌 회피를 위한 저장 파일명
     * @param filePath         저장소 내 상대 경로
     * @param contentType      MIME 타입
     * @param fileSize         파일 크기 (바이트)
     * @return 생성된 미디어
     */
    public static Media upload(String originalFilename, String storedFilename, String filePath,
            String contentType, long fileSize) {
        return new Media(originalFilename, storedFilename, filePath, contentType, fileSize);
    }
}
