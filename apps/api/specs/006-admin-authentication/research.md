# Research: Admin Authentication

## 1. Session 저장 방식

**Decision**: Spring Session을 도입하지 않고 Servlet Container 기본 `HttpSession` + Spring Security의 `SecurityContextRepository`(`HttpSessionSecurityContextRepository`, Spring Security 6+에서는 `DelegatingSecurityContextRepository` 기본 조합)를 사용한다.

**Rationale**: spec.md Assumptions에서 "Redis 기반 분산 Session은 이번 Phase 범위 밖"으로 명시했다. 현재 단일 서버 배포 구조이며 `spring-session-data-redis` 의존성도 아직 없다. 기본 `HttpSession` 기반으로도 FR-003(Session 생성), FR-004(Session Fixation 방지), FR-008(로그아웃 시 무효화) 요구사항을 모두 충족할 수 있다.

**Alternatives considered**:
- Spring Session + Redis: 다중 인스턴스 배포 시 필요하지만 현재 스케일과 spec 범위에 맞지 않음. `spring-boot-starter-data-redis`가 이미 의존성에 있으므로 이후 확장 시 `spring-session-data-redis`만 추가하면 전환 가능 — 이 경로를 research 단계에서 열어둔다.
- JWT 기반 무상태 인증: spec.md에서 명시적으로 배제(Out of Scope).

## 2. 로그인 처리 방식

**Decision**: Spring Security의 `UsernamePasswordAuthenticationFilter` 커스텀 대신, 커스텀 `AuthController`에서 `AuthenticationManager.authenticate(...)`를 직접 호출하고, 성공 시 `SecurityContextHolder`에 인증 정보를 설정한 뒤 `HttpSessionSecurityContextRepository.saveContext(...)`로 세션에 저장하는 방식(Servlet 3 API 기반 명시적 로그인)을 사용한다.

**Rationale**: spec.md는 "서버가 제공하는 HTML 로그인 페이지 또는 Form Login UI는 사용하지 않는다"와 "로그인 Request Body는 JSON 형식"을 명시했다. `formLogin()`의 기본 필터는 `application/x-www-form-urlencoded`와 Redirect 응답을 전제로 하므로 그대로 사용하면 계약을 위반한다. 컨트롤러에서 직접 `AuthenticationManager`를 호출하면 JSON 요청/응답을 온전히 제어하면서 Spring Security의 인증 검증(`UserDetailsService` + `PasswordEncoder`)은 그대로 재사용할 수 있다.

**Alternatives considered**:
- `formLogin()` + `successHandler`/`failureHandler` 커스터마이징: Redirect 억제는 가능하지만 요청 파싱 방식이 form-urlencoded 전제라 JSON 계약과 맞지 않아 필터를 다시 감싸야 하는 번거로움이 큼.
- API Key/Basic Auth: spec.md의 Session 요구사항과 무관.

## 3. Session Fixation 방어

**Decision**: 로그인 성공 시 `HttpServletRequest.changeSessionId()`를 호출(Spring Security의 `SessionAuthenticationStrategy` 중 `ChangeSessionIdAuthenticationStrategy` 사용)해 기존 세션 ID를 교체하고 인증 정보를 새 세션에 저장한다.

**Rationale**: FR-004 요구사항을 만족하는 표준 Spring Security 6+ 기본 전략이다. 세션 자체를 무효화하고 새로 생성하는 `migrateSession`/`newSession` 전략보다 `changeSessionId`가 서블릿 컨테이너 레벨에서 세션 속성을 보존하면서 ID만 교체해 더 가볍다.

**Alternatives considered**: `session.invalidate()` 후 `session = request.getSession(true)`로 완전히 새 세션 생성 — 더 강력하지만 이번 스코프에서 세션에 보존해야 할 사전 상태가 없어 과함.

## 4. CSRF 보호 방식

**Decision**: `CsrfConfigurer`를 활성화하고 `CookieCsrfTokenRepository.withHttpOnlyFalse()`를 사용해 CSRF 토큰을 `XSRF-TOKEN` 쿠키로 발급한다. Frontend는 이 쿠키 값을 읽어 상태 변경 요청(POST/PUT/PATCH/DELETE) 헤더(`X-XSRF-TOKEN`)에 실어 보낸다. CSRF 토큰 자체는 인증 여부와 무관하게 최초 요청 시 발급되도록 `CsrfTokenRequestAttributeHandler`를 커스텀하지 않고 기본 지연 로딩(deferred token) 동작을 사용한다.

**Rationale**: spec.md는 "Frontend가 필요한 CSRF Token을 얻고 이후 변경 요청에 전달할 수 있는 구조를 제공"만 요구하고 구체 전달 방식은 Plan 단계에 위임했다. Cookie 기반 double-submit 패턴은 Spring Security가 공식 지원하는 표준 방식이며 별도 서버 상태 저장이 필요 없어 세션 저장소 부담이 없다. `HttpOnly=false`는 Frontend JavaScript가 쿠키 값을 읽어 헤더에 실어야 하므로 CSRF 토큰 쿠키에 한해 의도적으로 적용한다(Session Cookie 자체와는 별개이며, Session Cookie는 그대로 `HttpOnly=true` 유지).

**Alternatives considered**: 세션에 CSRF 토큰을 저장하는 기본 `HttpSessionCsrfTokenRepository` — 매 요청 세션 접근 비용과 세션 고정 문제 재발 가능성이 있어 Cookie 방식보다 이번 구조에 덜 적합.

## 5. 비밀번호 인코딩

**Decision**: `BCryptPasswordEncoder`(기본 strength 10)를 `PasswordEncoder` 빈으로 등록한다.

**Rationale**: Spring Security 표준 기본값이며 FR-013(안전한 단방향 해시)을 충족하는 검증된 알고리즘이다. 별도 요구사항(강도 상향 등)이 spec에 없으므로 기본값을 사용한다.

**Alternatives considered**: Argon2 — 더 최신이지만 이번 프로젝트 규모에서 별도 네이티브 의존성을 추가할 이유가 없음.

## 6. 초기 관리자 계정 Provisioning

**Decision**: `ApplicationRunner` 빈(`admin` 패키지, `AdminInitializer`)에서 애플리케이션 기동 시 `admins` 테이블에 등록된 계정이 하나도 없으면, 환경 변수(`ADMIN_INIT_EMAIL`, `ADMIN_INIT_PASSWORD`)로 주어진 계정을 1회 생성한다. 이미 계정이 하나 이상 존재하면 아무 것도 하지 않는다.

**Rationale**: spec.md FR-017과 Admin Account Provisioning 절이 "회원가입 API가 되어서는 안 된다"와 "애플리케이션 초기화, 운영 설정, 데이터 초기화 절차 등 관리 가능한 방식"을 요구한다. 환경 변수 기반 `ApplicationRunner`는 외부에 노출되는 HTTP endpoint가 없어 요구사항을 직접 충족하며, 로컬 개발과 운영 배포 모두에서 재현 가능하다.

**Alternatives considered**: 고정 해시 비밀번호를 SQL로 미리 INSERT — 비밀번호를 코드/스크립트 파일에 고정하면 로그·버전관리 이력에 남아 FR-014(비밀번호 노출 금지)의 정신에 반하므로 배제. 별도 CLI 도구 — 이번 스코프에 비해 과함.

## 7. CORS Origin 관리

**Decision**: `application.yaml`에 `app.security.allowed-origins` 리스트 프로퍼티를 정의하고, 이를 `@ConfigurationProperties`로 바인딩해 `CorsConfigurationSource`에 주입한다. `allowCredentials(true)`와 함께 사용하며 `allowedOriginPatterns` 대신 명시적 `allowedOrigins` 목록만 사용한다(와일드카드 금지).

**Rationale**: spec.md FR-016이 "임의의 모든 Origin을 허용하지 않아야 한다"를 명시했고, `allowCredentials(true)`와 `allowedOrigins("*")`는 Spring 자체에서도 조합이 금지되어 있다. 환경별(dev/prod) 프로퍼티 파일로 분리 관리하면 배포 도메인이 바뀌어도 코드 변경 없이 대응 가능하다.

**Alternatives considered**: 코드에 하드코딩된 Origin 목록 — 환경별 대응이 어려워 배제.

## 8. 401/403 응답 통합

**Decision**: `SecurityConfig`의 `exceptionHandling()`에 기존 `authenticationEntryPoint`(401, 이미 구현됨) 외에 `accessDeniedHandler`(403)를 추가한다. 두 핸들러 모두 기존 `ApiResponse.failure(ErrorResponse.of(...))` 포맷을 그대로 사용하되, `ErrorCode`에 로그인 실패 전용 코드(`INVALID_CREDENTIALS` 또는 `AUTHENTICATION_FAILED`)를 추가해 일반 `UNAUTHORIZED`(세션 없음)와 로그인 자격 증명 실패를 구분한다.

**Rationale**: 기존 `GlobalExceptionHandler`/`ErrorCode`/`ApiResponse` 자산을 재사용하는 것이 "일관된 보안 정책으로 처리"(spec.md Business Rules)에 부합한다. `FORBIDDEN` ErrorCode는 이미 정의되어 있으나 미사용 상태였고, 이번에 `accessDeniedHandler`에서 처음 사용하게 된다.

**Alternatives considered**: `GlobalExceptionHandler`에서 `AuthenticationException`/`AccessDeniedException`을 `@ExceptionHandler`로 처리 — Spring Security의 예외는 필터 체인에서 발생해 `DispatcherServlet`의 `@ExceptionHandler`에 도달하지 못하므로 반드시 `exceptionHandling()`의 EntryPoint/Handler로 처리해야 함(기술적으로 필수, 대안 아님).

## 9. shared/CLAUDE.md 정합성

**Decision**: `shared/CLAUDE.md`의 "JWT 구조(클레임 파싱, 서명 검증)는 security 안에서만 처리" 문구를 Session 인증 기준으로 갱신한다(JWT → Session/SecurityContext로 대체 서술). 이는 코드 변경이 아니라 문서 정합성 문제이며 Phase 06 구현 완료 후 CLAUDE.md 유지 규칙에 따라 함께 반영한다.

**Rationale**: CLAUDE.md는 "코드에서 유추할 수 없는 비명시적 규칙"을 담아야 하는데, 현재 문구는 이번 Phase가 채택하지 않은 JWT를 전제로 해 실제 구현과 어긋난다. 문서가 틀린 상태로 남으면 다음 Phase 작업자가 잘못된 전제로 판단하게 된다.

**Note**: `docs/slice/PHASE_06_admin_authentication.md` 파일은 실제로는 Phase 01(Shared Foundation) 내용이 잘못 채워져 있음을 조사 중 확인했다. 이 파일은 이번 Plan의 근거로 사용하지 않았으며(`specs/006-admin-authentication/spec.md`만을 근거로 함), 수정 여부는 사용자 확인이 필요해 이번 구현 범위에 포함하지 않는다.

## 10. Flyway 미사용, ddl-auto 기반 스키마 관리

**Decision**: 이 프로젝트는 Flyway를 채택하지 않는다. `src/main/resources/db/migration/`에 있던 `V4`, `V5` SQL 파일은 build.gradle에 Flyway 의존성 자체가 없는 상태로 남아있던 죽은 파일(다른 프로젝트 템플릿을 복제하는 과정에서 잘못 포함된 잔재)로 확인되어 제거한다. `admins` 테이블도 마이그레이션 SQL 없이, 기존 category/work와 동일하게 JPA 엔티티 매핑 + `ddl-auto`로 생성한다.

**Rationale**: 지금까지 이 프로젝트의 실제 스키마 관리 방식은 dev의 `ddl-auto`(이번 Phase에서 `update`→`create`로 변경, 아래 참고)였고 Flyway는 한 번도 활성화된 적이 없다. 존재하지 않는 도구를 이번 Phase에서 갑자기 전제하는 대신, 이미 검증된 `ddl-auto` 관례를 그대로 따르는 것이 일관성 있고 불필요한 의존성 추가도 피할 수 있다.

**dev `ddl-auto` 값 변경**: `application-dev.yaml`은 이번 Phase에서 `update`에서 `create`로 변경한다(사용자 결정 — 개발 초기 단계라 재기동마다 스키마를 새로 만드는 편이 더 편함, 로컬 데이터 보존보다 스키마 최신성을 우선). `create`는 매 기동마다 기존 테이블을 drop 후 재생성하므로 로컬 데이터가 매번 사라지는 것이 정상 동작이다. prod는 운영 데이터를 보존해야 하므로 `update`를 유지한다 — dev와 prod의 `ddl-auto` 값이 이제 서로 다르다는 점에 유의.

**Alternatives considered**: Flyway를 이번 기회에 도입 — 스키마 변경 이력 관리 측면에서 이점이 있으나, 이번 Phase의 스코프(Admin 인증)를 벗어나는 별도의 인프라 결정이라 사용자 판단에 따라 배제했다. 필요해지면 별도 이슈로 다룬다.

## 11. application-prod.yaml 재작성

**Decision**: 기존 `application-prod.yaml`은 이 프로젝트와 무관한 다른 프로젝트(SQLite `thermo_logger_v2.db`, WebSocket TRACE 로깅 등 온도 로거 설정)의 잔재로 확인되었다. 사용자 확인에 따라 이 파일은 없는 것으로 간주하고, 이번 Phase에서 이 프로젝트 기준으로 새로 작성한다. 새 `application-prod.yaml`은 `application-dev.yaml`과 동일하게 PostgreSQL 기반으로 하되, 운영 환경에 필요한 값만 다르게 설정한다: Session Cookie `Secure=true`, `SameSite=Lax`(또는 Frontend/Backend가 서로 다른 서브도메인이면 `None`+`Secure`), `app.security.allowed-origins`에 실제 운영 Frontend 도메인, `spring.jpa.hibernate.ddl-auto: update`(운영 데이터 보존 목적 — dev는 `create`로 매 기동마다 스키마를 재생성하지만 prod는 재기동 시 기존 데이터가 삭제되면 안 되므로 반드시 `update`를 유지한다, #10 참고).

**Rationale**: 기존 파일 내용을 그대로 두면 Phase 06에서 추가하는 Cookie/CORS 운영 설정을 넣을 위치가 없거나, 무관한 설정과 뒤섞여 혼란을 유발한다. 파일을 이 프로젝트 기준으로 재작성하는 편이 Session 인증 도입의 운영 배포 요구사항(Cookie 보안 속성, 신뢰 Origin)을 명확히 충족한다.

**Alternatives considered**: 기존 파일을 그대로 두고 새 키만 병합 — SQLite/thermo_logger 설정과 PostgreSQL 기반 이 프로젝트 설정이 공존하면 실수로 잘못된 datasource가 활성화될 위험이 있어 배제.
