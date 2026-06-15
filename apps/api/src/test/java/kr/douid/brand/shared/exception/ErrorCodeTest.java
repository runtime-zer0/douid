package kr.douid.brand.shared.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void 모든_ErrorCode는_유효한_HTTP_상태와_비어있지않은_code_message를_가진다(ErrorCode errorCode) {
        assertThat(errorCode.getStatus()).isNotNull();
        assertThat(errorCode.getCode()).isNotBlank();
        assertThat(errorCode.getDefaultMessage()).isNotBlank();
    }

    @Test
    void INVALID_INPUT은_400() {
        assertThat(ErrorCode.INVALID_INPUT.getStatus().value()).isEqualTo(400);
    }

    @Test
    void INTERNAL_SERVER_ERROR는_500() {
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value()).isEqualTo(500);
    }
}
