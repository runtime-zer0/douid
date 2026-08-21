# Implementation Plan — Phase 06 Admin Authentication

**Branch**: `006-admin-authentication` | **Date**: 2026-08-21 | **Spec**: [spec.md](./spec.md)

## Summary

관리자 로그인·로그아웃·현재 관리자 조회를 Spring Security 기반 Session Authentication + REST JSON으로 구현하고, 기존 Category/Media/Work Admin API 앞에 인증(401)·인가(403) 경계를 추가한다. Public API는 기존과 동일하게 인증 없이 접근 가능한 상태를 유지한다. `SecurityConfig`를 STATELESS에서 Session 기반으로 전환하고, CSRF·CORS 정책을 신규로 도입한다. 기존 Work/Category/Media의 도메인·비즈니스 로직은 변경하지 않는다.

## Technical Context

- **Language/Version**: Java 25
- **Framework**: Spring Boot 4.0.6, Spring Security 7(starter-security 이미 포함), Spring Web MVC, Spring Data JPA
- **Storage**: PostgreSQL (Testcontainers로 테스트), `admins` 테이블 신규 추가
- **Session**: Servlet Container 기본 `HttpSession` (Spring Session/Redis 미도입 — [research.md](./research.md) #1)
- **Password Encoding**: `BCryptPasswordEncoder`
- **Testing**: JUnit 5, AssertJ, Mockito, `@WebMvcTest`, `@DataJpaTest` + Testcontainers, `spring-boot-starter-security-test`(이미 포함)
- **Project Type**: REST Web Service (feature-first 패키지 구조, `auth`/`admin` 패키지 이미 존재하며 각각 `CLAUDE.md`로 경계 정의됨)
- **Shared Foundation 재사용**: `ApiResponse<T>`, `ErrorResponse`, `ErrorCode`, `DomainException`, `GlobalExceptionHandler`, `BaseTimeEntity`
- **Pattern reference**: `category` feature 패키지 구조(domain/application/infrastructure/presentation)를 동일하게 따름
- **Constraints**: `SecurityConfig`의 기존 `csrf().disable()` + `STATELESS` 설정을 Session 기반으로 전환해야 하며, 이는 기존 모든 Admin API 호출부(테스트 포함)에 인증 헤더/쿠키가 필요해지는 파급 효과가 있음
- **Scale/Scope**: 단일 서버, 관리자 계정 소수(1인 프로젝트 특성상 사실상 1개)

## Constitution Check

*apps/api CLAUDE.md의 아키텍처 규칙을 게이트로 사용한다 (`.specify/memory/constitution.md`는 프로젝트 미작성 템플릿 상태).*

| 규칙 | 상태 | 근거 |
|---|---|---|
| Feature-first 패키지 구조 | PASS | `auth/{domain,application,infrastructure,presentation}` 신규 구현, `admin/{application,presentation}` 확장 — 두 패키지 모두 기존 `CLAUDE.md`에 경계가 이미 정의되어 있음 |
| domain이 Spring Security API에 의존하지 않음 (FR-018) | PASS | `Admin`(domain)는 `PasswordEncoder`/`UserDetails` 미참조. `AuthenticationService`(application)가 `PasswordEncoder.matches()` 호출 — [data-model.md](./data-model.md) Architecture Note |
| Work/Category/Media Domain 무변경 | PASS | 이번 Phase는 `SecurityConfig`의 URL 패턴 레벨 경계만 추가, 각 feature 코드 변경 없음 |
| controller가 repository 직접 호출 금지 | PASS | `AuthController` → `AuthenticationService`만 호출 |
| application이 HTTP DTO/외부 client에 직접 의존 안 함 | PASS | `AuthenticationService`는 `LoginCommand` DTO만 받고, `HttpServletRequest`(Session 재발급용)는 presentation에서 처리 후 결과만 넘김 |
| shared에 feature-specific 예외 금지 | PASS | `AuthenticationFailedException` 등은 `auth/domain`에 위치, `shared.exception`은 프레임워크 중립 타입만 유지 |
| Cross-feature 의존은 application port로만 | PASS | `auth`/`admin`은 Work/Category/Media의 application port를 호출하지 않음(이번 Phase에서 협력 불필요) |
| JavaDoc 대상 준수 | PASS | 신규 public/protected 메서드에 작성, JPA 기본 생성자·record 제외 |
| ApiResponse\<T\> 응답 래핑 | PASS | 로그인/로그아웃/me 모두 `ResponseEntity<ApiResponse<T>>` |
| 401/403 명확 구분 (FR-009, FR-010) | PASS | `authenticationEntryPoint`(기존) + `accessDeniedHandler`(신규) 분리 — [research.md](./research.md) #8 |
| 비밀번호 원문 미노출 (FR-013, FR-014) | PASS | `BCryptPasswordEncoder`, `Admin` 응답 DTO에 `passwordHash` 필드 없음 |

**Gate 결과**: 모두 통과. Phase 1 진행 가능.

**참고 — 문서 정합성 이슈(코드 변경 아님)**: `shared/CLAUDE.md`에 남아있는 "JWT 구조는 security 안에서만 처리" 문구는 이번 Phase가 채택한 Session 인증과 어긋난다. 구현 완료 후 CLAUDE.md 유지 규칙에 따라 Session 기준으로 정정한다([research.md](./research.md) #9). 또한 `docs/slice/PHASE_06_admin_authentication.md`는 실제로 Phase 01 내용이 잘못 채워져 있음을 조사 중 확인했으며, 이번 Plan은 이 파일을 근거로 삼지 않고 `spec.md`만을 근거로 한다 — 해당 파일 정정 여부는 이번 구현 범위 밖이므로 별도로 사용자 확인이 필요하다.

## Project Structure

### Documentation (this feature)

```text
specs/006-admin-authentication/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/
│   └── auth-api.md
├── checklists/
│   └── requirements.md
└── tasks.md              # Phase 2 output (/speckit.tasks)
```

### Source Code

```text
src/main/java/kr/douid/brand/
├── auth/
│   ├── domain/
│   │   ├── Admin.java
│   │   ├── AdminRole.java
│   │   ├── AdminRepository.java
│   │   ├── AuthErrorCode.java
│   │   └── AuthenticationFailedException.java
│   ├── application/
│   │   ├── AuthenticationService.java
│   │   ├── LoginCommand.java
│   │   └── AdminResult.java
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── AdminJpaRepository.java
│   │   │   └── JpaAdminRepositoryAdapter.java
│   │   └── security/
│   │       └── AdminUserDetailsService.java
│   └── presentation/
│       ├── AuthController.java
│       ├── request/
│       │   └── LoginRequest.java
│       └── response/
│           ├── AdminResponse.java
│           └── CsrfTokenResponse.java
├── admin/
│   └── application/
│       └── AdminInitializer.java
└── shared/
    ├── config/
    │   ├── SecurityConfig.java          # 기존 파일 수정 (Session 전환, CSRF/CORS 추가)
    │   └── CorsProperties.java          # 신규 — 허용 Origin 목록 바인딩
    └── security/
        ├── JsonAuthenticationEntryPoint.java   # 기존 SecurityConfig 내부 메서드 분리
        └── JsonAccessDeniedHandler.java        # 신규 — 403 처리

src/main/resources/
├── application.yaml       # app.security.allowed-origins 프로퍼티 추가, JPA ddl-auto가 admins 테이블 자동 생성
├── application-dev.yaml   # ADMIN_INIT_* 환경변수 기반 로컬 기본값 안내(선택)
└── application-prod.yaml  # 신규 작성 — 이 프로젝트 기준 운영 프로필 (기존 파일은 다른 프로젝트 잔재로 간주하고 대체)

src/test/java/kr/douid/brand/
├── auth/
│   ├── domain/
│   │   └── AdminTest.java
│   ├── application/
│   │   └── AuthenticationServiceTest.java
│   ├── presentation/
│   │   └── AuthControllerTest.java
│   └── infrastructure/
│       └── JpaAdminRepositoryTest.java
├── admin/
│   └── application/
│       └── AdminInitializerTest.java
└── shared/
    └── config/
        └── SecurityConfigTest.java       # Admin API 401/403, Public API 무영향 통합 검증
```

**Structure Decision**: 기존 `category`/`work` feature와 동일한 4계층 구조(domain/application/infrastructure/presentation)를 `auth`에 적용한다. `admin` 패키지는 이미 정의된 경계(admin 전용 진입 정책, provisioning)에 따라 `AdminInitializer`만 이번 Phase에서 담당하고, "현재 관리자 조회"는 인증 흐름 자체이므로 `auth.presentation.AuthController`에 포함한다(별도 admin query로 분리하지 않음 — spec.md Story 4가 로그인 상태 확인이라는 인증 관심사이지 admin 운영 조회가 아니기 때문). `shared.security`는 그동안 비어있던 위치를 이번 Phase에서 처음 채운다.

## Complexity Tracking

> Constitution Check 위반 없음. 기록 불필요.

## Risks & Decisions

| 리스크 | 대응 |
|---|---|
| 기존 `SecurityConfig`가 `STATELESS`+`csrf disable`이었기 때문에, Session 전환 시 기존 Admin API 통합 테스트가 모두 401로 깨질 수 있음 | 기존 Category/Media/Work의 `@WebMvcTest`는 Security 컨텍스트를 부분 mocking하므로 영향 적음. `@SpringBootTest` 통합 테스트가 있다면 `@WithMockUser(roles="ADMIN")` 또는 로그인 흐름을 먼저 태우는 방식으로 갱신 필요 — tasks 단계에서 기존 테스트 스캔 후 영향 범위 확정 |
| CSRF 활성화로 기존 Admin API의 상태 변경 테스트(MockMvc)가 403으로 실패할 수 있음 | MockMvc 테스트는 `.with(csrf())`(Spring Security Test 지원)를 추가해 우회. `spring-boot-starter-security-test`가 이미 의존성에 포함되어 있어 추가 의존성 불필요 |
| 초기 관리자 계정 환경변수가 설정되지 않은 상태로 배포되면 로그인 자체가 불가능 | `AdminInitializer`가 환경변수 미설정 시 경고 로그만 남기고 애플리케이션 기동은 정상 진행(기동 실패로 만들지 않음) — 배포 문서(quickstart.md)에 사전 준비 단계로 명시 |
| 기존 `application-prod.yaml`이 다른 프로젝트(SQLite/thermo_logger) 설정으로 이 프로젝트와 무관함 | 없는 파일로 간주하고 이번 Phase에서 이 프로젝트 기준으로 새로 작성한다(PostgreSQL, Session Cookie `Secure`/`SameSite`, CORS allowed-origins 등). 기존 내용은 전량 대체 대상이며 되살릴 이유가 없다 |
| 기존 `db/migration/` 디렉토리(V4/V5)가 사용되지 않는 Flyway 잔재였음(build.gradle에 Flyway 의존성 자체가 없음, 다른 프로젝트 템플릿 복제 과정에서 잘못 포함됨) | Flyway는 이 프로젝트에서 채택하지 않는다. 디렉토리 전체를 제거하고, `admins` 테이블을 포함한 전체 스키마를 JPA 엔티티 매핑 + `ddl-auto`로 생성한다. 마이그레이션 SQL은 작성하지 않는다. dev는 재기동마다 스키마를 새로 만드는 `create`, prod는 운영 데이터 보존을 위해 `update`로 분리한다 |
