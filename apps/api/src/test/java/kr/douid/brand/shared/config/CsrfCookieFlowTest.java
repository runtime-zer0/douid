package kr.douid.brand.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;
import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AdminRole;

/**
 * 실제 XSRF-TOKEN 쿠키를 발급받아 상태 변경 요청에 사용하는 흐름을 검증
 *
 * {@code SecurityMockMvcRequestPostProcessors.csrf()}는 실제 필터체인의 {@code CsrfFilter}가 들고 있는
 * {@code tokenRepository} 필드를 리플렉션으로 세션 기반 repository로 교체한다.
 * {@code @SpringBootTest}는 클래스 단위로 컨텍스트를 공유하므로, 같은 클래스 안에서 {@code .with(csrf())}를
 * 사용하는 다른 테스트와 함께 두면 실행 순서에 따라 {@code CookieCsrfTokenRepository} 기반 쿠키 발급 검증이
 * 오염될 수 있어 별도 클래스로 분리한다
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CsrfCookieFlowTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void CSRF_토큰_발급후_헤더에_실어보내면_상태변경_요청_정상처리() throws Exception {
        String adminEmail = "admin-csrf-cookie@douid.kr";
        adminRepository.save(Admin.create(adminEmail,
                passwordEncoder.encode("password"), AdminRole.ADMIN));

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = csrfResult.getResponse().getContentAsString();
        String headerName = JsonPath.read(responseBody, "$.data.headerName");
        String csrfToken = JsonPath.read(responseBody, "$.data.token");
        Cookie[] csrfCookies = csrfResult.getResponse().getCookies();
        assertThat(csrfCookies).isNotEmpty();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookies)
                        .header(headerName, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password\"}".formatted(adminEmail)))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/api/admin/categories")
                        .session(session)
                        .cookie(csrfCookies)
                        .header(headerName, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"브랜딩","slug":"branding-csrf","displayOrder":1,"visible":true}
                                """))
                .andExpect(status().isCreated());
    }
}
