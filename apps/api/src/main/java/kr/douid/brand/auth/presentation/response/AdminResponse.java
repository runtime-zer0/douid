package kr.douid.brand.auth.presentation.response;

import kr.douid.brand.auth.application.AdminResult;
import kr.douid.brand.auth.domain.AdminRole;

public record AdminResponse(Long id, String email, AdminRole role) {

    /**
     * 결과 모델을 응답으로 변환
     *
     * @param result 변환할 결과 모델
     * @return 변환된 응답
     */
    public static AdminResponse from(AdminResult result) {
        return new AdminResponse(result.id(), result.email(), result.role());
    }
}
