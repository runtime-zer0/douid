package kr.douid.brand.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRepository;
import kr.douid.brand.auth.domain.AdminRole;
import kr.douid.brand.category.domain.Category;
import kr.douid.brand.category.domain.CategoryRepository;
import kr.douid.brand.work.domain.Work;
import kr.douid.brand.work.domain.WorkRepository;
import kr.douid.brand.work.domain.WorkVisibility;

/**
 * Session 기반 인증 유지, Admin/Public API 접근 경계를 검증
 *
 * MockMvc는 실제 서블릿 컨테이너처럼 브라우저 쿠키를 왕복시키지 않으므로,
 * 로그인 요청에서 생성된 {@link MockHttpSession}을 다음 요청에 그대로 실어 세션 유지를 검증한다
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityConfigTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

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

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WorkRepository workRepository;

    private String adminEmail;
    private String categorySlug;
    private String workSlug;

    @BeforeEach
    void setUp() {
        int sequence = SEQUENCE.incrementAndGet();
        adminEmail = "admin-%d@douid.kr".formatted(sequence);
        adminRepository.save(Admin.create(adminEmail,
                passwordEncoder.encode("password"), AdminRole.ADMIN));

        categorySlug = "category-%d".formatted(sequence);
        Category category = categoryRepository.save(
                Category.create("카테고리 %d".formatted(sequence), categorySlug, 0, true));

        workSlug = "work-%d".formatted(sequence);
        workRepository.save(Work.create("작업물 %d".formatted(sequence), workSlug,
                "요약", "설명", category.getId(), WorkVisibility.VISIBLE));
    }

    @Test
    void login_이후_세션으로_보호된_API_접근_유지() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(get("/api/admin/categories").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void 세션_없이_보호된_API_접근시_401() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호_불일치로_로그인시_401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"wrong-password\"}".formatted(adminEmail)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 존재하지_않는_계정으로_로그인시_401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@douid.kr\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void ADMIN_권한_없이_보호된_API_접근시_403() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증없이_비로그인상태로_여러_Admin_API_호출시_모두_401() throws Exception {
        mockMvc.perform(get("/api/admin/works")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/media/1").with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    void 로그인_전후_세션ID가_교체됨_Session_Fixation_방어() throws Exception {
        MvcResult beforeLogin = mockMvc.perform(get("/api/public/categories")).andReturn();
        String sessionIdBeforeLogin = beforeLogin.getRequest().getSession(true).getId();

        MockHttpSession sessionAfterLogin = login();

        assertThat(sessionAfterLogin.getId()).isNotEqualTo(sessionIdBeforeLogin);
    }

    @Test
    void 인증없이_공개_Work_목록_및_상세_카테고리별_목록_조회시_200() throws Exception {
        mockMvc.perform(get("/api/public/works"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/works/{slug}", workSlug))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/categories/{categorySlug}/works", categorySlug))
                .andExpect(status().isOk());
    }

    @Test
    void 인증없이_공개_카테고리_목록_조회시_200() throws Exception {
        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void CSRF_토큰_없이_로그인된_세션으로_상태변경_요청시_403() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/admin/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"브랜딩","slug":"branding","displayOrder":1,"visible":true}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void 허용되지_않은_Origin에서_인증_요청시_CORS_차단() throws Exception {
        mockMvc.perform(get("/api/public/categories")
                        .header("Origin", "https://malicious.example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 로그아웃_이후_동일세션으로_재요청시_401() throws Exception {
        MockHttpSession session = login();

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password\"}".formatted(adminEmail)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
