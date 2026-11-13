package kr.douid.brand.shared.config;

import java.util.List;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.auth.application.AuthenticationService;
import kr.douid.brand.shared.security.CustomAccessDeniedHandler;
import kr.douid.brand.shared.security.CustomAuthenticationEntryPoint;
import kr.douid.brand.shared.security.CustomLoginAuthenticationFilter;
import kr.douid.brand.shared.security.CustomLoginFailureHandler;
import kr.douid.brand.shared.security.CustomLoginSuccessHandler;
import kr.douid.brand.shared.security.CustomLogoutSuccessHandler;

/**
 * Spring Security 필터 체인 설정
 *
 * Session 기반 인증 정책 사용
 * public/admin 경로 분리, 미인증 401·인가 실패 403 JSON 응답 처리
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    /**
     * 보안 설정 객체 생성
     *
     * @param objectMapper   401/403 응답 직렬화에 사용할 Jackson ObjectMapper
     * @param corsProperties credential 포함 요청을 허용할 Origin 목록
     */
    public SecurityConfig(ObjectMapper objectMapper, CorsProperties corsProperties) {
        this.objectMapper = objectMapper;
        this.corsProperties = corsProperties;
    }

    /**
     * 비밀번호 인코더 빈 등록
     *
     * @return {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 필터 체인 내부에서 공유할 {@link AuthenticationManager} 노출
     *
     * @param configuration Spring Security의 기본 인증 설정
     * @return 구성된 {@link AuthenticationManager}
     * @throws Exception 빈 조회 실패 시
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 인증 정보를 세션에 저장·복원하는 저장소 노출
     *
     * 로그인 성공 핸들러와 필터 체인이 동일한 인스턴스를 공유해야 한다
     *
     * @return 세션 기반 {@link SecurityContextRepository}
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * HTTP 보안 필터 체인 구성
     *
     * @param http                     HttpSecurity 빌더
     * @param authenticationManager    로그인 필터가 위임할 인증 관리자
     * @param securityContextRepository 로그인 성공 시 인증 정보를 저장할 저장소
     * @param authenticationService    로그인 성공 시 관리자 정보를 조회할 서비스
     * @return 구성된 {@link SecurityFilterChain}
     * @throws Exception 필터 체인 구성 실패 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AuthenticationService authenticationService) throws Exception {
        CustomLoginAuthenticationFilter loginFilter = new CustomLoginAuthenticationFilter(
                authenticationManager,
                objectMapper,
                new CustomLoginSuccessHandler(authenticationService, securityContextRepository, objectMapper),
                new CustomLoginFailureHandler(objectMapper));

        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.POST, "/api/public/ai-chat/stream")))
                // 지연된 헤더 쓰기와 첫 SSE flush의 Tomcat 헤더 변경 경합 방지
                .headers(headers -> headers.withObjectPostProcessor(new ObjectPostProcessor<HeaderWriterFilter>() {
                    /** 보안 헤더를 downstream 비동기 쓰기 전에 고정
                     * @param filter 보안 헤더 필터
                     * @param <O> 필터 타입
                     * @return 시점이 조정된 필터
                     */
                    @Override
                    public <O extends HeaderWriterFilter> O postProcess(O filter) {
                        filter.setShouldWriteHeadersEagerly(true);
                        return filter;
                    }
                }))
                .cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(request -> request.getDispatcherType() == DispatcherType.ERROR
                                && "POST".equals(request.getMethod())
                                && "/error".equals(request.getServletPath())
                                && (request.getContextPath() + "/api/public/ai-chat/stream")
                                        .equals(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))).permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/media/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/csrf-token").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new CustomLogoutSuccessHandler(objectMapper)))
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * credential 포함 요청을 허용할 CORS 정책 구성
     *
     * @return 등록된 Origin에 한해 credential 포함 요청을 허용하는 {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
