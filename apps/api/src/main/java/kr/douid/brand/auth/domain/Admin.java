package kr.douid.brand.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.douid.brand.shared.entity.BaseTimeEntity;
import lombok.Getter;

@Getter
@Entity
@Table(name = "admins")
public class Admin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Column(nullable = false)
    private boolean active;

    protected Admin() {
    }

    private Admin(String email, String passwordHash, AdminRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
    }

    /**
     * 새 관리자 계정을 생성
     *
     * @param email 로그인 식별자
     * @param passwordHash 해시된 비밀번호 (원문 비밀번호는 domain에 전달하지 않는다)
     * @param role 관리자 권한
     * @return 생성된 관리자 계정
     */
    public static Admin create(String email, String passwordHash, AdminRole role) {
        return new Admin(email, passwordHash, role);
    }
}
