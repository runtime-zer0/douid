package kr.douid.brand.work.infrastructure.query;

import kr.douid.brand.work.application.query.WorkMediaView;
import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * QueryDSL 조회용 WorkMedia + Media flat projection
 *
 * {@link WorkMediaView} 변환 후 application 계층 반환
 */
public record WorkMediaProjection(
        Long mediaId,
        WorkMediaRole role,
        Integer sortOrder,
        String altText,
        String filePath,
        String originalFilename) {

    /**
     * projection을 application query view로 변환
     *
     * @return 모든 필드가 null이면 null, 아니면 변환된 {@link WorkMediaView}
     */
    public WorkMediaView toView() {
        if (mediaId == null) {
            return null;
        }
        return new WorkMediaView(
                mediaId,
                role,
                sortOrder == null ? 0 : sortOrder,
                altText,
                filePath,
                originalFilename);
    }
}
