# Data Model: Admin Authentication

## Admin

관리자 인증 계정. `auth` feature의 Aggregate Root.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto-increment | 관리자 식별자 |
| email | String | not null, unique | 로그인 식별자 |
| passwordHash | String | not null | BCrypt 해시. 원문 비밀번호는 저장하지 않는다 |
| role | AdminRole | not null | MVP에서는 `ADMIN` 고정 |
| active | boolean | not null, default true | 비활성화 시 로그인 불가 |
| createdAt | LocalDateTime | not null, auto | `BaseTimeEntity` 상속 |
| updatedAt | LocalDateTime | not null, auto | `BaseTimeEntity` 상속 |

**테이블명**: `admins` (기존 `categories`/`works`와 동일하게 복수형 명사 관례를 따른다)

**생성 규칙**: `Admin.create(email, rawPassword, passwordEncoder)` 형태의 static factory로 생성 시점부터 해시된 비밀번호만 보유하도록 강제한다. 기본 생성자는 JPA용 `protected`.

**Domain method**:
- `boolean matchesPassword(String rawPassword, PasswordEncoder encoder)` — 로그인 검증에 사용. domain이 `PasswordEncoder`(Spring Security 인터페이스)에 의존하는 것은 CLAUDE.md의 "domain이 인증 기술을 알 필요 없음" 원칙과 충돌하므로, 실제로는 **application layer(`AuthenticationService`)가 `PasswordEncoder.matches(rawPassword, admin.getPasswordHash())`를 직접 호출**하고, domain은 `getPasswordHash()`/`isActive()` 같은 순수 접근자만 제공한다. (아래 Architecture Note 참고)
- `boolean isActive()` — 비활성 계정 여부 확인.

**상태 전이**: 없음(이번 Phase 범위에서 활성/비활성 전환 API는 제공하지 않음 — Out of Scope의 "관리자 활동 감사 로그", "관리자 대시보드"와 함께 향후 확장 대상).

## AdminRole

```java
public enum AdminRole {
    ADMIN
}
```

Spring Security `GrantedAuthority`로 변환 시 `ROLE_ADMIN` 접두사를 붙인다. 다중 role 확장을 대비해 enum으로 두되, MVP는 단일 값만 갖는다.

## AuthenticationSession

별도 엔티티/테이블이 아니다. Servlet Container의 `HttpSession` + Spring Security `SecurityContext`로 표현되는 런타임 개념이며, 영속화 대상이 아니므로 data-model에서는 개념 정의만 남긴다.

- 생성 시점: 로그인 성공 시 `changeSessionId()`로 세션 ID 교체 후 `SecurityContext`를 세션에 저장.
- 소멸 시점: 로그아웃 시 `SecurityContextLogoutHandler`가 `HttpSession.invalidate()` 호출.
- 식별자 교환 수단: Session Cookie(`JSESSIONID`, `HttpOnly`, `SameSite=Lax` 이상).

## Architecture Note — Domain과 Spring Security 의존성 분리

spec.md FR-018과 apps/api CLAUDE.md의 Architecture Rules("도메인 모델은 인증 기술을 알 필요가 없다")에 따라 다음 경계를 유지한다.

```
presentation (AuthController)
     ↓ (LoginCommand)
application (AuthenticationService)
     ↓ AdminRepository.findByEmail()
     ↓ PasswordEncoder.matches(rawPassword, admin.getPasswordHash())   ← Spring Security 타입은 application까지만
domain (Admin)
     ↑ getPasswordHash(), isActive(), getRole()   ← 순수 접근자만 노출
```

- `Admin`(domain)는 `PasswordEncoder`, `UserDetails`, `GrantedAuthority` 등 Spring Security 타입을 import하지 않는다.
- `UserDetailsService` 구현체(`AdminUserDetailsService`)는 `auth.infrastructure.security`에 두고, `AdminRepository`(domain port)를 통해 조회한 `Admin`을 Spring Security의 `UserDetails`로 변환하는 어댑터 역할만 한다.
- `Work`, `Category`, `Media` Domain은 이번 Phase에서 어떤 코드 변경도 없다(접근 제어는 `SecurityConfig`의 URL 패턴 레벨에서만 추가되므로 각 feature의 domain/application 코드는 무영향).

## Relationships

- `Admin`은 다른 feature(Work/Category/Media) 엔티티와 어떤 관계도 맺지 않는다. 완전히 독립된 Aggregate다.
- `auth` ↔ `admin` 패키지 역할 구분: `auth`는 로그인/로그아웃/인증 흐름(`AuthenticationService`, `Admin`, `AdminRepository`)을 소유하고, `admin`은 "현재 관리자 조회"처럼 admin 전용 조회 진입점과 초기 계정 provisioning(`AdminInitializer`)을 소유한다. 두 패키지 모두 `admin/CLAUDE.md`, `auth/CLAUDE.md`에 이미 정의된 경계를 따른다.

## Validation Rules

- 로그인 요청(email, password)은 둘 다 공백/빈 문자열 불가 (`@NotBlank`).
- email은 이메일 형식 검증(`@Email`)을 적용하되, 형식 오류도 다른 인증 실패와 동일한 401 응답으로 귀결시켜 계정 존재 여부 추론을 막는다(FR-005). 단, JSON 파싱 실패나 필드 누락 같은 구조적 오류는 기존 `MethodArgumentNotValidException` 400 처리 경로를 그대로 따른다(FR-001의 "잘못된 요청 데이터"에 해당, 인증 실패와는 다른 계층의 오류).
- `email`은 DB unique 제약으로 중복 생성을 막는다(관리자 계정 생성 자체는 API로 노출하지 않으므로 provisioning 스크립트 수준에서만 해당).
