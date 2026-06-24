package kr.douid.brand.shared.exception;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void 모든_ErrorCode는_비어있지않은_code_message를_가진다(ErrorCode errorCode) {
        assertThat(errorCode.getCode()).isNotBlank();
        assertThat(errorCode.getDefaultMessage()).isNotBlank();
    }

    @Test
    void INVALID_INPUT은_공통_입력_오류_코드를_가진다() {
        assertThat(ErrorCode.INVALID_INPUT.getCode()).isEqualTo("INVALID_INPUT");
    }

    @Test
    void INTERNAL_SERVER_ERROR는_공통_서버_오류_코드를_가진다() {
        assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo("INTERNAL_ERROR");
    }
}
