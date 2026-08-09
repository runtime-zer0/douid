package kr.douid.brand.work.application.query;

import kr.douid.brand.work.domain.WorkMediaRole;

/**
 * WorkMedia와 Media를 조합한 조회 결과
 *
 * @param mediaId           Media 식별자
 * @param role               Work에서 이 Media가 사용되는 역할
 * @param sortOrder          정렬 순서
 * @param altText            대체 텍스트 (nullable)
 * @param filePath           저장소 내 경로 (파일 서빙 URL 조합에 사용)
 * @param originalFilename   원본 파일명
 */
public record WorkMediaView(
        Long mediaId,
        WorkMediaRole role,
        int sortOrder,
        String altText,
        String filePath,
        String originalFilename) {
}
