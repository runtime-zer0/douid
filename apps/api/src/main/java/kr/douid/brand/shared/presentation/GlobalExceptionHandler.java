package kr.douid.brand.shared.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.douid.brand.shared.exception.DomainException;
import kr.douid.brand.shared.exception.ErrorCode;
import kr.douid.brand.shared.exception.IntegrationException;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * 애플리케이션 전역 예외를 HTTP API 응답으로 변환하는 presentation adapter
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인 규칙 위반 예외 응답 변환
     *
     * @param e 발생한 {@link DomainException}
     * @return 예외에 정의된 HTTP 상태와 오류 본문
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDomainException(DomainException e) {
        ErrorResponse body = ErrorResponse.of(e.getCode(), e.getMessage());
        return ResponseEntity.status(ErrorHttpStatusMapper.from(e.getType()))
                .body(ApiResponse.failure(body));
    }

    /**
     * 외부 연동 실패 예외 응답 변환
     *
     * @param e 발생한 {@link IntegrationException}
     * @return 예외에 정의된 HTTP 상태와 오류 본문
     */
    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIntegrationException(
            IntegrationException e) {
        ErrorResponse body = ErrorResponse.of(e.getCode(), e.getMessage());
        return ResponseEntity.status(ErrorHttpStatusMapper.from(e.getType()))
                .body(ApiResponse.error(body));
    }

    /**
     * 검증 실패 예외 응답 변환
     *
     * @param e {@code @Valid} 검증 실패 시 발생하는 예외
     * @return 400 응답과 필드별 오류 목록
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getDefaultMessage(), fieldErrors);

        return ResponseEntity.badRequest().body(ApiResponse.failure(body));
    }

    /**
     * 처리되지 않은 예외의 500 응답 변환
     *
     * @param e 처리되지 않은 예외
     * @return 500 응답과 내부 서버 오류 본문
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        return ResponseEntity.internalServerError().body(ApiResponse.error(body));
    }

}
