package kr.douid.brand.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminTest {

    @Test
    void create_정상_생성() {
        Admin admin = Admin.create("admin@douid.kr", "hashed-password", AdminRole.ADMIN);

        assertThat(admin.getEmail()).isEqualTo("admin@douid.kr");
        assertThat(admin.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(admin.getRole()).isEqualTo(AdminRole.ADMIN);
    }

    @Test
    void create_기본_활성_상태() {
        Admin admin = Admin.create("admin@douid.kr", "hashed-password", AdminRole.ADMIN);

        assertThat(admin.isActive()).isTrue();
    }
}
