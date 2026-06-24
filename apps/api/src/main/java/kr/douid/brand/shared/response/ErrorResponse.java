package kr.douid.brand.shared.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * API 오류 응답 본문
 *
 * 필드 오류가 없으면 {@code fields}는 직렬화에서 제외
 */
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> fields;

    /**
     * 오류 응답 객체 생성
     *
     * @param code 오류 코드 문자열
     * @param message 오류 메시지
     * @param fields 필드 오류 목록
     */
    private ErrorResponse(String code, String message, List<FieldError> fields) {
        this.code = code;
        this.message = message;
        this.fields = fields;
    }

    /**
     * 코드·메시지 직접 지정 오류 응답 생성
     *
     * @param code    오류 코드 문자열
     * @param message 오류 메시지
     * @return 필드 오류 없는 오류 응답
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }

    /**
     * 코드·메시지·필드 오류 직접 지정 오류 응답 생성
     *
     * @param code    오류 코드 문자열
     * @param message 오류 메시지
     * @param fields  필드별 오류 목록
     * @return 필드 오류가 포함된 오류 응답
     */
    public static ErrorResponse of(String code, String message, List<FieldError> fields) {
        return new ErrorResponse(code, message, fields);
    }

    /**
     * Bean Validation 필드 오류를 담는 중첩 클래스
     */
    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        /**
         * 필드 오류 객체 생성
         *
         * @param field   오류가 발생한 필드명
         * @param message 해당 필드의 오류 메시지
         */
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
