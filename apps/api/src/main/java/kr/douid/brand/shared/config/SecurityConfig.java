package kr.douid.brand.shared.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import tools.jackson.databind.ObjectMapper;

import kr.douid.brand.shared.exception.ErrorCode;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.response.ErrorResponse;

/**
 * Spring Security 필터 체인 설정
 *
 * 세션 없는 stateless 정책 사용
 * public/admin 경로 분리 및 미인증 401 JSON 응답 처리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    /**
     * 보안 설정 객체 생성
     *
     * @param objectMapper 401 응답 직렬화에 사용할 Jackson ObjectMapper
     */
    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleUnauthorized));

        return http.build();
    }

    /**
     * 미인증 요청의 401 JSON 응답 작성
     *
     * @param request       HTTP 요청
     * @param response      HTTP 응답
     * @param authException 인증 실패 예외
     * @throws IOException 응답 쓰기 실패 시
     */
    private void handleUnauthorized(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException
    ) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<ErrorResponse> body = ApiResponse.failure(ErrorResponse.of(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getDefaultMessage()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
