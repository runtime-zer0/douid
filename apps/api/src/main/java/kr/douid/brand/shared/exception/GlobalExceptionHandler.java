package kr.douid.brand.shared.exception;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * 애플리케이션 전역 예외를 {@link ApiResponse} 형식으로 변환하는 핸들러
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 응답 변환
     *
     * @param e 발생한 {@link BusinessException}
     * @return {@link ErrorCode}에 정의된 HTTP 상태와 오류 본문
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse body = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.failure(body));
    }

    /**
     * 검증 실패 예외 응답 변환
     *
     * @param e {@code @Valid} 검증 실패 시 발생하는 예외
     * @return 400 응답과 필드별 오류 목록
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(body));
    }

    /**
     * 처리되지 않은 예외의 500 응답 변환
     *
     * @param e 처리되지 않은 예외
     * @return 500 응답과 내부 서버 오류 본문
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(body));
    }
}
