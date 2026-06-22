package kr.douid.brand.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 표준 래퍼
 *
 * {@code SUCCESS} {@code FAILURE} {@code ERROR} 상태만 사용
 * {@code null} 필드는 직렬화에서 제외
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public enum Status { SUCCESS, FAILURE, ERROR }

    private final Status status;
    private final T data;
    private final String message;

    /**
     * API 응답 객체 생성
     *
     * @param status 응답 상태
     * @param data 응답 데이터
     * @param message 응답 메시지
     */
    private ApiResponse(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    /**
     * 응답 데이터 포함 성공 응답 생성
     *
     * @param <T>  payload 타입
     * @param data 응답 데이터
     * @return {@code SUCCESS} 상태의 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Status.SUCCESS, data, null);
    }

    /**
     * 응답 데이터 없는 성공 응답 생성
     *
     * @param <T> payload 타입
     * @return {@code SUCCESS} 상태의 빈 응답
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(Status.SUCCESS, null, null);
    }

    /**
     * 오류 데이터 포함 실패 응답 생성
     *
     * @param <T>  오류 데이터 타입
     * @param data 오류 상세 데이터
     * @return {@code FAILURE} 상태의 응답
     */
    public static <T> ApiResponse<T> failure(T data) {
        return new ApiResponse<>(Status.FAILURE, data, null);
    }

    /**
     * 오류 메시지 포함 실패 응답 생성
     *
     * @param <T>     payload 타입
     * @param message 오류 메시지
     * @return {@code FAILURE} 상태의 응답
     */
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(Status.FAILURE, null, message);
    }

    /**
     * 오류 데이터 포함 서버 오류 응답 생성
     *
     * @param <T>  오류 데이터 타입
     * @param data 오류 상세 데이터
     * @return {@code ERROR} 상태의 응답
     */
    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(Status.ERROR, data, null);
    }

    /**
     * 오류 메시지 포함 서버 오류 응답 생성
     *
     * @param <T>     payload 타입
     * @param message 오류 메시지
     * @return {@code ERROR} 상태의 응답
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(Status.ERROR, null, message);
    }
}
