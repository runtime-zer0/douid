package kr.douid.brand.media.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미디어 도메인 오류 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum MediaErrorCode {

    MEDIA_NOT_FOUND("MEDIA_NOT_FOUND", "미디어를 찾을 수 없습니다."),
    MEDIA_SAVE_FAILED("MEDIA_SAVE_FAILED", "미디어 저장에 실패했습니다."),
    INVALID_FILE_TYPE("INVALID_FILE_TYPE", "이미지 파일만 업로드할 수 있습니다."),
    EMPTY_FILE("EMPTY_FILE", "빈 파일은 업로드할 수 없습니다."),
    MEDIA_IN_USE("MEDIA_IN_USE", "작업물에서 사용 중인 미디어는 삭제할 수 없습니다.");

    private final String code;
    private final String defaultMessage;
}
