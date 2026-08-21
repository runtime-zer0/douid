package kr.douid.brand.shared.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.shared.security.JsonAccessDeniedHandler;
import kr.douid.brand.shared.security.JsonAuthenticationEntryPoint;

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
     * 로그인 컨트롤러가 직접 사용할 {@link AuthenticationManager} 노출
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
     * HTTP 보안 필터 체인 구성
     *
     * @param http HttpSecurity 빌더
     * @return 구성된 {@link SecurityFilterChain}
     * @throws Exception 필터 체인 구성 실패 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .cors(Customizer.withDefaults())
                .securityContext(context ->
                        context.securityContextRepository(new HttpSessionSecurityContextRepository()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/media/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/csrf-token").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper)));

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
