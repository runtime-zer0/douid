package kr.douid.brand.shared.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.douid.brand.shared.response.ApiResponse;

/**
 * 서버 상태 확인 엔드포인트
 */
@RestController
@RequestMapping("/api/public")
public class HealthController {

    /**
     * 서버 상태 확인 응답 반환
     *
     * @return {@code "ok"} 문자열을 담은 200 응답
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("ok"));
    }
}
