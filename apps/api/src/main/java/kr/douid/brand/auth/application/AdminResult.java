package kr.douid.brand.auth.application;

import kr.douid.brand.auth.domain.Admin;
import kr.douid.brand.auth.domain.AdminRole;

public record AdminResult(Long id, String email, AdminRole role) {

    /**
     * 관리자 엔티티를 결과 모델로 변환
     *
     * @param admin 변환할 관리자 엔티티
     * @return 변환된 결과 모델
     */
    public static AdminResult from(Admin admin) {
        return new AdminResult(admin.getId(), admin.getEmail(), admin.getRole());
    }
}
