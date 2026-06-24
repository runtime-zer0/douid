package kr.douid.brand.shared.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import kr.douid.brand.shared.exception.DomainErrorType;
import kr.douid.brand.shared.exception.DomainException;
import kr.douid.brand.shared.exception.IntegrationErrorType;
import kr.douid.brand.shared.exception.IntegrationException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void DomainException_발생시_해당_코드와_HTTP_상태_반환() throws Exception {
        mockMvc.perform(get("/test/domain-exception"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.data.code").value("TEST_NOT_FOUND"));
    }

    @Test
    void IntegrationException_발생시_해당_코드와_HTTP_상태_반환() throws Exception {
        mockMvc.perform(get("/test/integration-exception"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.data.code").value("TEST_EXTERNAL_FAILURE"));
    }

    @Test
    void 검증_실패시_400과_필드_오류_목록_반환() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILURE"))
                .andExpect(jsonPath("$.data.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.data.fields").isArray());
    }

    @Test
    void 알수없는_예외_발생시_500과_내부정보_미노출() throws Exception {
        mockMvc.perform(get("/test/unknown-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.data.code").value("INTERNAL_ERROR"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/domain-exception")
        void throwDomain() {
            throw new TestDomainException();
        }

        @GetMapping("/test/integration-exception")
        void throwIntegration() {
            throw new TestIntegrationException();
        }

        @PostMapping("/test/validation")
        void throwValidation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/unknown-exception")
        void throwUnknown() {
            throw new RuntimeException("unexpected error");
        }
    }

    static class TestDomainException extends DomainException {
        TestDomainException() {
            super(DomainErrorType.NOT_FOUND, "TEST_NOT_FOUND", "테스트 도메인 예외");
        }
    }

    static class TestIntegrationException extends IntegrationException {
        TestIntegrationException() {
            super(IntegrationErrorType.BAD_GATEWAY, "TEST_EXTERNAL_FAILURE", "테스트 연동 예외");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
