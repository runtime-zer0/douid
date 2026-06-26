package kr.douid.brand.work.domain;

/**
 * WorkMedia 교체에 필요한 미디어 항목 값 객체
 *
 * @param mediaId   미디어 ID
 * @param role      Work에서의 미디어 역할
 * @param sortOrder 정렬 순서
 * @param altText   대체 텍스트
 */
public record WorkMediaItem(Long mediaId, WorkMediaRole role, int sortOrder, String altText) {
}
