package kr.douid.brand.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    public enum Status { SUCCESS, FAILURE, ERROR }

    private final Status status;
    private final T data;
    private final String message;

    private ApiResponse(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Status.SUCCESS, data, null);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(Status.SUCCESS, null, null);
    }

    public static <T> ApiResponse<T> failure(T data) {
        return new ApiResponse<>(Status.FAILURE, data, null);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(Status.FAILURE, null, message);
    }

    public static <T> ApiResponse<T> error(T data) {
        return new ApiResponse<>(Status.ERROR, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(Status.ERROR, null, message);
    }
}
