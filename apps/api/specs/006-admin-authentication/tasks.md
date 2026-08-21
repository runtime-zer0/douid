# Tasks: Admin Authentication

**Input**: Design documents from `/specs/006-admin-authentication/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

**Organization**: User Story 우선순위(P1→P2) 기준으로 독립 구현·검증 가능하도록 구성. Story 간 의존은 대부분 `SecurityConfig`/`AuthController` 등 공유 파일에 순차 추가되는 형태로 나타난다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (서로 다른 파일, 미완료 태스크 미의존)
- **[Story]**: 해당 태스크가 속한 User Story (US1~US7)
- 각 태스크에 정확한 파일 경로 포함

---

## Phase 1: Setup (공유 인프라)

**Purpose**: 사용하지 않는 Flyway 잔재 제거, 운영 설정 파일 준비

- [X] T001 `src/main/resources/db/migration/` 디렉토리 제거 — build.gradle에 Flyway 의존성이 없는 상태로 남아있던 다른 프로젝트 템플릿 잔재(`V4`, `V5`). 이 프로젝트는 Flyway를 채택하지 않으며, `admins` 테이블도 기존 관례대로 JPA 엔티티 매핑 + `ddl-auto`로 생성한다(research.md #10)
- [X] T002 `src/main/resources/application-prod.yaml` 재작성 — 기존 파일(다른 프로젝트 잔재)을 이 프로젝트 기준으로 전량 대체: PostgreSQL datasource, `spring.jpa.hibernate.ddl-auto: update`(운영 데이터 보존 목적 — dev의 `create`와 다르게 설정, research.md #10)
- [X] T003 `src/main/resources/application.yaml` 수정 — `app.security.allowed-origins` 프로퍼티 추가 (dev 기본값은 로컬 Frontend origin)
- [X] T004 `src/main/resources/application-prod.yaml` 수정 — 운영용 `app.security.allowed-origins`, `server.servlet.session.cookie.secure: true`, `server.servlet.session.cookie.same-site: lax` 설정 추가 (T002 의존)

---

## Phase 2: Foundational (모든 User Story가 의존하는 기반)

**Purpose**: Admin 도메인 타입, Repository port, 예외 체계, CORS 프로퍼티 바인딩 정의 — 이 Phase가 완료되어야 US1~US7 구현 가능

**⚠️ CRITICAL**: 이 Phase가 완료되기 전에는 어떤 User Story 작업도 시작할 수 없음

- [X] T005 [P] `src/main/java/kr/douid/brand/auth/domain/AdminRole.java` 생성 — `ADMIN` enum
- [X] T006 [P] `src/main/java/kr/douid/brand/auth/domain/AuthErrorCode.java` 생성 — `AUTHENTICATION_FAILED` 오류 코드 enum (`DomainErrorType.UNAUTHORIZED` 매핑)
- [X] T007 `src/main/java/kr/douid/brand/auth/domain/Admin.java` 생성 — `@Entity`, `@Table(name = "admins")`, `BaseTimeEntity` 상속, `email`/`passwordHash`/`role`/`active` 필드, `Admin.create(email, passwordHash, role)` static factory, `getPasswordHash()`/`isActive()`/`getRole()` 접근자만 노출 (Spring Security 타입 미의존, T005 의존)
- [X] T008 [P] `src/main/java/kr/douid/brand/auth/domain/AuthenticationFailedException.java` 생성 — `DomainException` 상속, `AuthErrorCode.AUTHENTICATION_FAILED` (T006 의존)
- [X] T009 `src/main/java/kr/douid/brand/auth/domain/AdminRepository.java` 생성 — `findByEmail(String email): Optional<Admin>`, `save`, `existsAny(): boolean` domain port interface (T007 의존)
- [X] T010 [P] `src/main/java/kr/douid/brand/shared/config/CorsProperties.java` 생성 — `@ConfigurationProperties(prefix = "app.security")`, `allowedOrigins: List<String>` 바인딩

**Checkpoint**: 도메인 핵심 타입 완성 — User Story 구현 시작 가능

---

## Phase 3: User Story 1 — 관리자 로그인 (Priority: P1) 🎯 MVP

**Goal**: 관리자가 email/password를 JSON으로 전달해 로그인하면 인증 Session이 생성되고 Session Cookie가 발급된다

**Independent Test**: `POST /api/auth/login`에 등록된 관리자 계정으로 요청 후 Session Cookie가 발급되고, 해당 쿠키로 보호된 Admin API 호출이 허용되는지 검증

### Infrastructure (US1에서 처음 필요)

- [X] T011 [P] [US1] `src/main/java/kr/douid/brand/auth/infrastructure/persistence/AdminJpaRepository.java` 생성 — `JpaRepository<Admin, Long>`, `findByEmail`, `existsBy...`(count 기반) 쿼리 메서드
- [X] T012 [US1] `src/main/java/kr/douid/brand/auth/infrastructure/persistence/JpaAdminRepositoryAdapter.java` 생성 — `AdminRepository` 구현, `AdminJpaRepository` 위임 (T011 의존)
- [X] T013 [P] [US1] `src/main/java/kr/douid/brand/auth/infrastructure/security/AdminUserDetailsService.java` 생성 — `UserDetailsService` 구현, `AdminRepository.findByEmail()` 조회 결과를 `UserDetails`로 변환(권한은 `ROLE_ADMIN`), 계정 미존재 시 `UsernameNotFoundException` (T009 의존)

### Application Layer

- [X] T014 [P] [US1] `src/main/java/kr/douid/brand/auth/application/LoginCommand.java` 생성 — `email`, `password` record
- [X] T015 [P] [US1] `src/main/java/kr/douid/brand/auth/application/AdminResult.java` 생성 — `id`, `email`, `role` record (password 필드 없음)
- [X] T016 [US1] `src/main/java/kr/douid/brand/auth/application/AuthenticationService.java` 생성 — `authenticate(LoginCommand): AdminResult` 메서드: `AuthenticationManager.authenticate()` 호출 → 실패 시 `AuthenticationFailedException` → 성공 시 `AdminResult` 반환 (T008, T013, T014, T015 의존)

### Presentation & Security 설정

- [X] T017 [P] [US1] `src/main/java/kr/douid/brand/auth/presentation/request/LoginRequest.java` 생성 — `@NotBlank @Email email`, `@NotBlank password`, `toCommand()` 포함
- [X] T018 [P] [US1] `src/main/java/kr/douid/brand/auth/presentation/response/AdminResponse.java` 생성 — `id`, `email`, `role` record
- [X] T019 [US1] `src/main/java/kr/douid/brand/auth/presentation/AuthController.java` 생성 — `POST /api/auth/login` 엔드포인트: `AuthenticationService.authenticate()` 호출 → 성공 시 `httpRequest.getSession(true)`로 세션을 먼저 생성한 뒤 `changeSessionId()`로 Session Fixation 방어(서블릿 스펙상 세션이 없으면 `changeSessionId()`가 예외를 던지므로 순서가 중요) → `SecurityContext`를 세션에 저장 → `ResponseEntity<ApiResponse<AdminResponse>>` 200 반환 (T016, T017, T018 의존)
- [X] T020 `src/main/java/kr/douid/brand/shared/config/SecurityConfig.java` 수정 — `PasswordEncoder`(`BCryptPasswordEncoder`) 빈 등록, `AuthenticationManager` 빈 노출, `sessionCreationPolicy`를 `STATELESS`에서 `IF_REQUIRED`로 변경, `authorizeHttpRequests`에 `/api/auth/login` permitAll 추가 (T013 의존)

### Tests

- [X] T021 [P] [US1] `src/test/java/kr/douid/brand/auth/domain/AdminTest.java` 생성 — `Admin.create()` 생성 검증, `isActive()` 기본값 단위 테스트
- [X] T022 [P] [US1] `src/test/java/kr/douid/brand/auth/application/AuthenticationServiceTest.java` 생성 (로그인 성공 케이스) — Mockito로 `AuthenticationManager` mocking, 정상 로그인 흐름 검증
- [X] T023 [P] [US1] `src/test/java/kr/douid/brand/auth/presentation/AuthControllerTest.java` 생성 (로그인 성공 케이스) — `@WebMvcTest` + MockMvc: 200 응답, HttpSession 생성 확인, 응답 본문에 password 미포함 검증 (`Set-Cookie` 헤더는 MockMvc가 실제 서블릿 컨테이너가 아니라 응답에 담지 않음 — 세션 유지 자체는 T030 `SecurityConfigTest`에서 `MockHttpSession` 재사용 방식으로 검증)

**Checkpoint**: `POST /api/auth/login`이 정상 동작하고 Session Cookie가 발급됨

---

## Phase 4: User Story 2 — 로그인 실패 처리 (Priority: P1)

**Goal**: 존재하지 않는 계정, 잘못된 비밀번호, 비활성화된 계정으로는 로그인에 실패하며 모두 동일한 응답을 반환한다

**Independent Test**: 세 가지 실패 시나리오로 각각 로그인 시도 후 동일한 401 응답 구조와 Session 미생성을 검증

### Application Layer

- [X] T024 [US2] `AuthenticationService.java` 수정 — `authenticate()`에서 `Admin.isActive() == false`인 경우도 `BadCredentialsException`(또는 동일 카테고리 예외)으로 처리되도록 `AdminUserDetailsService`의 `UserDetails.isEnabled()` 반환값과 연계 확인 (T013, T016 의존) — Phase 3에서 `authenticationManager.authenticate()` 실패를 전부 `AuthenticationFailedException`으로 통일하는 catch 블록으로 이미 반영됨
- [X] T025 [US2] `AdminUserDetailsService.java` 수정 — `UserDetails` 생성 시 `admin.isActive()`를 `enabled` 플래그로 전달해 비활성 계정이 `DisabledException`으로 통일 처리되도록 함 (T013 의존) — Phase 3에서 `.disabled(!admin.isActive())`로 이미 반영됨

### Presentation

- [X] T026 [US2] `AuthController.java` 수정 — `AuthenticationException`(계정 미존재/비번오류/비활성 모두 포함) catch 시 `AuthErrorCode.AUTHENTICATION_FAILED`로 통일된 401 응답 반환, Session 미생성 보장 (T019 의존) — `AuthenticationFailedException`이 `DomainException`을 상속해 `GlobalExceptionHandler`가 통일 처리, 로그인 실패 시 세션 생성 코드에 도달하지 않으므로 Session 미생성 보장됨

### Tests

- [X] T027 [P] [US2] `AuthenticationServiceTest.java` 수정 (실패 케이스 추가) — 계정 미존재, 비밀번호 불일치, 비활성 계정 세 케이스 모두 동일한 예외 타입/메시지로 귀결되는지 검증
- [X] T028 [P] [US2] `AuthControllerTest.java` 수정 (실패 케이스 추가) — 세 케이스 모두 동일한 401 JSON 응답(code, message, status) 검증, 잘못된 JSON/누락 필드는 400 검증 — 401 케이스는 Phase 3의 `login_인증실패_401`, 400 케이스는 `login_이메일_형식_오류_400`으로 이미 반영됨

**Checkpoint**: 로그인 실패 3가지 케이스가 모두 동일한 응답으로 통일됨

---

## Phase 5: User Story 3 — Session 기반 인증 유지 (Priority: P1)

**Goal**: 로그인 후 발급된 Session Cookie만으로 이후 Admin API 요청의 인증 상태가 유지된다

**Independent Test**: 로그인 후 Session Cookie로 별도 보호된 Admin API를 연속 호출해 인증 상태가 유지되는지, Cookie 없이는 거부되는지 검증

### Security 설정

- [X] T029 [US3] `SecurityConfig.java` 수정 — `securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))` 명시적 등록으로 세션 기반 `SecurityContext` 복원 보장 (T020 의존)

### Tests

- [X] T030 [US3] `src/test/java/kr/douid/brand/shared/config/SecurityConfigTest.java` 생성 — `@SpringBootTest` + MockMvc: 로그인 성공 후 생성된 `MockHttpSession`을 재사용해 임의의 보호된 Admin API(`/api/admin/categories` GET) 호출 시 인증 상태 유지 확인, Session 없이 호출 시 401 확인, 로그인 전후 Session ID가 달라짐(Session Fixation 방어) 확인 (MockMvc는 실제 서블릿 컨테이너가 아니라 `Set-Cookie` 헤더를 발급하지 않으므로 Cookie 문자열 대신 `MockHttpSession` 객체 자체를 다음 요청에 실어 검증)

**Checkpoint**: Session Cookie 기반 인증 유지가 검증됨

---

## Phase 6: User Story 4 — 현재 관리자 정보 조회 (Priority: P2)

**Goal**: 인증된 관리자가 자신의 최소 정보(식별자, email, role)를 조회할 수 있다

**Independent Test**: 로그인 상태와 비로그인 상태 각각에서 `GET /api/auth/me` 호출 후 응답 차이 검증

### Application & Presentation

- [X] T031 [US4] `AuthenticationService.java` 수정 — `getCurrentAdmin(): AdminResult` 메서드 추가: `SecurityContextHolder`에서 인증 주체 조회 → `AdminRepository.findByEmail()` → `AdminResult` 반환 (T009, T016 의존) — Phase 3에서 `AuthenticationService` 작성 시 함께 반영됨
- [X] T032 [US4] `AuthController.java` 수정 — `GET /api/auth/me` 엔드포인트 추가, `authenticated()` 필요 (T031 의존)
- [X] T033 `SecurityConfig.java` 수정 — `authorizeHttpRequests`에 `/api/auth/**` authenticated 명시 추가 (`/api/auth/login`이 그 앞에 permitAll로 우선 매칭되어 로그인만 예외 처리됨, T020 의존)

### Tests

- [X] T034 [P] [US4] `AuthenticationServiceTest.java` 수정 (getCurrentAdmin 케이스 추가) — 인증 컨텍스트 mocking 후 정상 조회 검증
- [X] T035 [P] [US4] `AuthControllerTest.java` 수정 (`/me` 케이스 추가) — 인증 시 200 + 관리자 정보, 비인증 시 401, 응답에 password 미포함 검증

**Checkpoint**: `GET /api/auth/me`가 인증 상태에 따라 올바르게 응답함

---

## Phase 7: User Story 5 — 관리자 로그아웃 (Priority: P2)

**Goal**: 로그인된 관리자가 로그아웃하면 현재 Session이 무효화되어 이후 재사용할 수 없다

**Independent Test**: 로그인 → 로그아웃 → 동일 Session Cookie로 Admin API 재호출 시 401 검증

### Presentation & Security 설정

- [X] T036 [US5] `AuthController.java` 수정 — `POST /api/auth/logout` 엔드포인트 추가: `SecurityContextLogoutHandler`로 `HttpSession.invalidate()` 및 `SecurityContextHolder.clearContext()` 수행 후 `ApiResponse` 200 반환 (T019 의존)
- [X] T037 `SecurityConfig.java` 수정 — `authorizeHttpRequests`에 `/api/auth/logout` authenticated 명시 추가, `logout()` DSL 대신 커스텀 컨트롤러 방식이므로 기본 `logout()` 설정은 비활성화 상태 유지 확인 (T020 의존) — T033에서 추가한 `/api/auth/**` authenticated 규칙으로 이미 커버됨, 기본 `logout()` DSL 미사용 확인

### Tests

- [X] T038 [US5] `AuthControllerTest.java` 수정 (로그아웃 케이스 추가) — 로그아웃 200 응답, 비인증 상태 로그아웃 요청 401 검증
- [X] T039 [US5] `SecurityConfigTest.java` 수정 — 로그인 → 로그아웃 → 동일 세션으로 `/api/auth/me` 재호출 시 401 확인 (T030 의존)

**Checkpoint**: 로그아웃 후 Session이 완전히 무효화됨

---

## Phase 8: User Story 6 — Admin API 접근 보호 (Priority: P1)

**Goal**: Category/Media/Work의 기존 Admin API가 인증(401)·인가(403) 경계로 보호된다

**Independent Test**: 비로그인/로그인·ADMIN 상태 각각으로 기존 Category/Media/Work Admin API를 호출해 거부/허용 여부 검증

### Security 설정

- [X] T040 [US6] `src/main/java/kr/douid/brand/shared/security/JsonAccessDeniedHandler.java` 생성 — `AccessDeniedHandler` 구현, 403 + `ApiResponse.failure(ErrorResponse.of(ErrorCode.FORBIDDEN...))` JSON 응답 (기존 `SecurityConfig`의 `handleUnauthorized` 메서드와 동일한 패턴)
- [X] T041 `SecurityConfig.java` 수정 — 기존 `handleUnauthorized` private 메서드를 `src/main/java/kr/douid/brand/shared/security/JsonAuthenticationEntryPoint.java`로 분리, `authorizeHttpRequests`의 `/api/admin/**`를 `hasRole("ADMIN")`으로 변경, `exceptionHandling()`에 `accessDeniedHandler(new JsonAccessDeniedHandler())` 추가 (T040 의존)
- [X] T042 `src/main/java/kr/douid/brand/shared/exception/ErrorCode.java` 확인 — 기존 `FORBIDDEN` 코드 재사용(신규 추가 불필요), 사용처 없던 코드가 T040에서 최초로 사용됨

### Tests

- [X] T043 [P] [US6] `SecurityConfigTest.java` 수정 — 비인증 상태로 `GET /api/admin/categories`, `GET /api/admin/works`, `DELETE /api/admin/media/{id}` 호출 시 모두 401 확인 (계획 시점엔 POST로 가정했으나, 실제 `MediaController`는 `/api/admin/media`에 GET 없이 업로드/삭제만 존재해 DELETE로 대체)
- [X] T044 [P] [US6] `SecurityConfigTest.java` 수정 — 인증되었으나 `ADMIN` role이 아닌 사용자로 동일 API 호출 시 403 확인 (`@WithMockUser(roles = "USER")` 활용)
- [X] T045 [US6] `SecurityConfigTest.java` 수정 — 로그인된 `ADMIN` 사용자로 `GET /api/admin/categories` 호출 시 기존 Phase 04/05 비즈니스 로직이 정상 실행되는지 확인 (`login_이후_세션으로_보호된_API_접근_유지`로 검증. GET 조회라 CSRF 토큰 불필요 — 상태 변경 API의 CSRF 통과 검증은 Phase 10에서 CSRF 활성화 이후 진행)

**Checkpoint**: 모든 기존 Admin API가 401/403 경계로 보호됨

---

## Phase 9: User Story 7 — Public API 공개 접근 유지 (Priority: P1)

**Goal**: 공개 Category/Work 조회 API는 인증 도입 전후 동일하게 인증 없이 접근 가능하다

**Independent Test**: 인증 정보 없이 기존 Public 조회 API를 호출해 정상 응답 확인 (회귀 테스트 성격)

### Tests

- [X] T046 [P] [US7] `SecurityConfigTest.java` 수정 — 실제 공개 Category/Work를 저장한 뒤 인증 정보 없이 `GET /api/public/works`, `GET /api/public/works/{slug}`, `GET /api/public/categories/{categorySlug}/works`, `GET /api/public/categories` 호출 시 모두 200 확인
- [X] T047 [P] [US7] 기존 `PublicWorkControllerTest`/`PublicCategoryControllerTest`(`@WebMvcTest` + `@Import(SecurityConfig.class)`) 재실행 — Phase 8의 `SecurityConfig` 변경(`/api/admin/**` hasRole 강화, exceptionHandling 추가)으로 인한 회귀 없음을 확인 (신규 코드 없음, 검증만 수행)

**Checkpoint**: Public API가 인증 도입 이후에도 회귀 없이 그대로 동작함

---

## Phase 10: CSRF & CORS (Cross-Cutting — 모든 상태 변경 API에 영향)

**Purpose**: spec.md FR-015(CSRF), FR-016(CORS)을 충족하는 공통 보안 설정. 특정 User Story 하나에 속하지 않고 US1~US6의 상태 변경 요청 전체에 적용되므로 별도 Phase로 분리한다.

- [X] T048 `SecurityConfig.java` 수정 — `csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))` 활성화 (기존 `csrf().disable()` 제거)
- [X] T049 [P] `src/main/java/kr/douid/brand/auth/presentation/response/CsrfTokenResponse.java` 생성 — `headerName`, `token` record
- [X] T050 `AuthController.java` 수정 — `GET /api/auth/csrf-token` 엔드포인트 추가: `request.getAttribute(CsrfToken.class.getName())`로 토큰 강제 리졸브 후 `CsrfTokenResponse` 반환 (T049 의존)
- [X] T051 `SecurityConfig.java` 수정 — `authorizeHttpRequests`에 `/api/auth/csrf-token` permitAll 추가
- [X] T052 `SecurityConfig.java` 수정 — `CorsConfigurationSource` 빈 추가: `CorsProperties.getAllowedOrigins()` 기반 `allowedOrigins` 설정, `allowCredentials(true)`, `cors(Customizer.withDefaults())` 적용 (T010 의존)
- [X] T053 기존 Admin API MockMvc 테스트 전체 점검 — `WorkControllerTest`, `AdminCategoryControllerTest`, `MediaControllerTest` 등 상태 변경 요청(POST/PUT/PATCH/DELETE)에 `.with(csrf())` 추가 (Spring Security Test 지원, 신규 의존성 불필요)
- [X] T054 [P] `src/test/java/kr/douid/brand/shared/config/SecurityConfigTest.java` 수정 — CSRF 토큰 없이 로그인된 세션으로 상태 변경 요청 시 403 확인, `csrf-token` 발급 후 헤더에 실어 보내면 정상 처리되는지 확인 (쿠키 기반 발급 검증은 `SecurityMockMvcRequestPostProcessors.csrf()`가 실제 필터체인의 `CsrfFilter.tokenRepository`를 세션 기반으로 오염시키는 문제를 피하기 위해 `CsrfCookieFlowTest`로 분리)
- [X] T055 [P] `SecurityConfigTest.java` 수정 — 허용되지 않은 Origin의 `Origin` 헤더를 포함한 요청이 CORS 정책으로 차단되는지 확인

**Checkpoint**: CSRF 보호와 CORS Origin 제한이 모든 상태 변경 API에 적용됨

---

## Phase 11: 초기 관리자 계정 Provisioning (Cross-Cutting)

**Purpose**: spec.md FR-017 — 회원가입 API 없이 초기 관리자 계정을 준비하는 절차. 특정 User Story에 속하지 않는 운영 준비 작업.

- [X] T056 `src/main/java/kr/douid/brand/admin/application/AdminInitializer.java` 생성 — `ApplicationRunner` 구현: `AdminRepository.existsAny()`가 false일 때만 `ADMIN_INIT_EMAIL`/`ADMIN_INIT_PASSWORD` 환경변수로 `Admin.create()` 후 저장, 환경변수 미설정 시 경고 로그만 남기고 기동 계속 (T007, T009, T012 의존)
- [X] T057 [P] `src/test/java/kr/douid/brand/admin/application/AdminInitializerTest.java` 생성 — Mockito: 계정 없을 때 생성, 계정 존재 시 스킵, 환경변수 미설정 시 예외 없이 경고 로그만 남기는지 검증

**Checkpoint**: 초기 관리자 계정이 회원가입 API 없이 준비됨

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: 전체 흐름 정합성 검토 및 문서 정리

- [X] T058 [P] `AuthController.java` — JavaDoc 보완: 클래스·public 메서드 JavaDoc 작성 (이미 반영되어 있음을 확인)
- [X] T059 [P] `AuthenticationService.java` — JavaDoc 보완: 클래스·public 메서드 JavaDoc 작성 (이미 반영되어 있음을 확인)
- [X] T060 [P] `AdminRepository.java`, `AdminInitializer.java` — JavaDoc 보완 (이미 반영되어 있음을 확인)
- [X] T061 [P] `JsonAuthenticationEntryPoint.java`, `JsonAccessDeniedHandler.java` — JavaDoc 보완 (이미 반영되어 있음을 확인)
- [X] T062 `apps/api/src/main/java/kr/douid/brand/shared/CLAUDE.md` 수정 — "JWT 구조는 security 안에서만 처리" 문구를 Session/SecurityContext 기준으로 정정 (research.md #9)
- [X] T063 `apps/api/CLAUDE.md` 정리 — `Active Technologies`/`Recent Changes` 섹션은 코드·git에서 유추 가능한 내용이라 CLAUDE.md 유지 규칙에 따라 섹션 자체를 삭제하고, 코드로 유추하기 어려운 비명시적 결정(Flyway 미채택, ddl-auto 기반 스키마 관리)만 `Stack` 섹션에 한 줄로 반영
- [X] T064 `quickstart.md` 시나리오 전체 수동 실행 — 로그인 → me → csrf-token → admin API 상태변경 → 로그아웃 → 재요청 401 흐름 최종 검증 (로그인이 CSRF 보호 대상인 점, 쿠키 raw 값이 아닌 응답 body의 masked 토큰을 헤더에 실어야 하는 점, 미인증 Admin API 호출이 CsrfFilter에 의해 403으로 먼저 차단되는 점을 반영해 문서 정정)

---

## Dependencies & Execution Order

### Phase 의존 관계

- **Phase 1 (Setup)**: 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 후 시작, 모든 US를 블로킹
- **Phase 3 (US1 로그인)**: Phase 2 완료 후 시작 — MVP 완성 기준
- **Phase 4 (US2 로그인 실패)**: Phase 3 완료 후 시작 (`AuthController`, `AuthenticationService` 파일 공유)
- **Phase 5 (US3 Session 유지)**: Phase 3 완료 후 시작 (`SecurityConfig` 공유, US2와 독립적으로 병행 가능)
- **Phase 6 (US4 현재 관리자 조회)**: Phase 3 완료 후 시작
- **Phase 7 (US5 로그아웃)**: Phase 6 완료 후 시작 (`AuthController` 파일 공유, `/me`와 로그아웃 모두 인증 필요 경로라 순서상 US4 다음이 자연스러움)
- **Phase 8 (US6 Admin API 보호)**: Phase 3 완료 후 시작 가능 (독립적) — 단, 실제 검증(T045)은 Phase 10 CSRF 활성화 이후 완전해짐
- **Phase 9 (US7 Public API 유지)**: Phase 8 완료 후 회귀 검증 성격으로 진행
- **Phase 10 (CSRF & CORS)**: Phase 8 완료 후 시작 (모든 상태 변경 API에 영향을 주므로 US6 보호 로직이 먼저 자리잡은 뒤 적용)
- **Phase 11 (초기 계정 Provisioning)**: Phase 2 완료 후 언제든 병행 가능 (다른 Phase와 파일 의존 없음)
- **Phase 12 (Polish)**: 나머지 모든 Phase 완료 후 시작

### User Story 의존 관계

- **US1 (P1)**: Phase 2 완료 후 독립 구현 가능 — 가장 먼저 구현되어야 하는 MVP
- **US2 (P1)**: US1과 `AuthenticationService`, `AuthController` 파일 공유 → US1 완료 후 순차 진행
- **US3 (P1)**: US1의 Session 발급 로직 위에서 검증하는 성격 → US1 완료 후 진행, US2와는 독립적
- **US4 (P2)**: US1의 인증 흐름 위에서 `/me` 추가 → US1 완료 후 진행
- **US5 (P2)**: US4와 `AuthController` 파일 공유, 인증 흐름을 전제 → US4 완료 후 순차 진행
- **US6 (P1)**: US1과 독립적으로 `SecurityConfig`만 수정 — 병행 가능하나 CSRF(Phase 10) 적용 전까지는 상태 변경 API 테스트가 완전하지 않음
- **US7 (P1)**: US6이 `SecurityConfig`를 변경한 뒤 회귀가 없는지 확인하는 성격 → US6 완료 후 진행

### Phase 내 병렬 가능 태스크

**Phase 2**: T005, T006 병렬 → T007 → T008 → T009, T010 병렬

**Phase 3**: T011, T013, T014, T015, T017, T018 병렬 → T012, T016, T019, T020 → T021, T022, T023 병렬

---

## Parallel Example: User Story 1 (Phase 3)

```
# T011, T013~T015, T017~T018 (Phase 3 내 병렬)
T011: AdminJpaRepository.java
T013: AdminUserDetailsService.java
T014: LoginCommand.java
T015: AdminResult.java
T017: LoginRequest.java
T018: AdminResponse.java
```

---

## Implementation Strategy

### MVP First (User Story 1 완성)

1. Phase 1 (Setup): T001–T004
2. Phase 2 (Foundational): T005–T010
3. Phase 3 (US1): T011–T023
4. **STOP & VALIDATE**: `POST /api/auth/login` 동작 확인, Session Cookie 발급 확인
5. 이후 Phase 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 순차 진행

### Incremental Delivery

1. Phase 1 + Phase 2 완료 → Admin 도메인 기반 완성
2. Phase 3 완료 → 로그인 API (MVP)
3. Phase 4 완료 → 로그인 실패 응답 일관성 확보
4. Phase 5 완료 → Session 유지 검증 확보
5. Phase 6 + 7 완료 → 현재 관리자 조회 + 로그아웃 완성
6. Phase 8 + 9 완료 → 기존 Admin API 보호 + Public API 회귀 없음 확인
7. Phase 10 완료 → CSRF/CORS 보안 경계 완성
8. Phase 11 완료 → 초기 관리자 계정 준비 절차 완성

---

## Notes

- `Admin`(domain)는 `PasswordEncoder`/`UserDetails` 등 Spring Security 타입을 import하지 않는다 — `AuthenticationService`(application)와 `AdminUserDetailsService`(infrastructure)에서만 처리한다 (data-model.md Architecture Note).
- Work/Category/Media의 domain·application 코드는 이번 tasks에서 전혀 수정하지 않는다. 오직 `SecurityConfig`의 URL 패턴 레벨 경계만 추가된다.
- Phase 8(US6) 이후 기존 Admin 관련 MockMvc 테스트(`WorkControllerTest` 등)가 인증 컨텍스트 없이 401로 실패할 수 있으므로, Phase 10의 CSRF 활성화와 함께 T053에서 일괄 점검한다.
- `application-prod.yaml`은 기존 파일 내용이 이 프로젝트와 무관함을 사용자가 확인했으므로, T002에서 전량 재작성한다(기존 내용 참고 없이 새로 작성).
- 이 프로젝트는 Flyway를 채택하지 않는다. `db/migration/`에 있던 V4/V5는 다른 프로젝트 템플릿 복제 과정에서 잘못 포함된 잔재로 확인되어 T001에서 제거하며, `admins` 테이블도 마이그레이션 SQL 없이 JPA 엔티티 매핑으로 생성한다.
- Testcontainers를 사용하는 테스트는 PostgreSQL 이미지가 필요하다 (기존 feature 테스트 설정 재사용).
