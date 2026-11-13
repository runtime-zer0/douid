package kr.douid.brand.shared.presentation;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

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
        var response = ResponseEntity.status(ErrorHttpStatusMapper.from(e.getType()))
                .contentType(MediaType.APPLICATION_JSON);
        if (e.getRetryAfterSeconds() != null) {
            response.header("Retry-After", e.getRetryAfterSeconds().toString());
        }
        return response.body(ApiResponse.failure(body));
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
                .contentType(MediaType.APPLICATION_JSON).body(ApiResponse.error(body));
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

        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(body));
    }

    /**
     * 파일 크기 초과 예외 응답 변환
     *
     * @param e multipart 파일 크기 제한 초과 시 발생하는 예외
     * @return 400 응답과 오류 본문
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.INVALID_INPUT.getCode(), "허용된 최대 파일 크기를 초과했습니다.");
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(body));
    }

    /**
     * DB constraint 위반 예외의 409 응답 변환
     *
     * 사전 검증을 통과한 뒤 동시성 race로 인해 발생한 충돌을 처리한다.
     *
     * @param e 발생한 {@link DataIntegrityViolationException}
     * @return 409 응답과 충돌 오류 본문
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.CONFLICT.getCode(),
                ErrorCode.CONFLICT.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(ApiResponse.failure(body));
    }

    /**
     * 표준 HTTP 입력 오류의 안전한 JSON 변환
     *
     * @param e HTTP 입력 오류
     * @return 원문 없는 오류 응답
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class, ResponseStatusException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleHttpInput(Exception e) {
        int status = e instanceof ResponseStatusException response ? response.getStatusCode().value()
                : e instanceof HttpMediaTypeNotSupportedException ? 415
                : e instanceof HttpMediaTypeNotAcceptableException ? 406 : 400;
        String code = switch (status) {
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 406 -> "NOT_ACCEPTABLE";
            default -> "INVALID_INPUT";
        };
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.failure(ErrorResponse.of(code, "요청 값이 올바르지 않습니다.")));
    }

    /**
     * 처리되지 않은 예외의 500 응답 변환
     *
     * @param response 커밋 상태를 확인할 응답
     * @param e 처리되지 않은 예외
     * @return 500 응답과 내부 서버 오류 본문
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e, HttpServletResponse response) {
        if (response.isCommitted()) {
            return null;
        }
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
        return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(ApiResponse.error(body));
    }

}
