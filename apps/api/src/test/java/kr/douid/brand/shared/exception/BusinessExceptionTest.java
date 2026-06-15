package kr.douid.brand.shared.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    void ErrorCode로_생성시_기본_메시지_보존() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.NOT_FOUND.getDefaultMessage());
    }

    @Test
    void 커스텀_메시지로_생성시_메시지_오버라이드() {
        BusinessException ex = new BusinessException(ErrorCode.NOT_FOUND, "작업물을 찾을 수 없습니다.");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("작업물을 찾을 수 없습니다.");
    }

    @Test
    void UNAUTHORIZED_에러코드는_401_상태() {
        BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED);

        assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
