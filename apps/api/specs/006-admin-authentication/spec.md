# Feature Specification: Admin Authentication

**Feature Branch**: `006-admin-authentication`
**Created**: 2026-08-21
**Status**: Draft
**Phase**: 06 — Admin Authentication

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 관리자 로그인 (Priority: P1)

관리자는 email과 password를 JSON 형식으로 전달해 로그인한다. 인증에 성공하면 서버는 새로운 인증 Session을 생성하고, 클라이언트가 이후 요청에서 사용할 Session Cookie를 발급한다. 로그인 성공 응답은 HTML이나 Redirect가 아닌 JSON이며, 로그인 성공 여부와 관리자 최소 정보를 담는다.

**Why this priority**: 인증 경계 전체가 로그인 성공 시 Session이 만들어진다는 전제 위에서 동작한다. 이 흐름이 없으면 나머지 모든 보호 기능이 검증 불가능하다.

**Independent Test**: 등록된 관리자 계정으로 로그인 요청을 보내고, 응답에 담긴 Session Cookie로 보호된 Admin API를 호출해 접근이 허용되는지 독립 검증할 수 있다.

**Acceptance Scenarios**:

1. **Given** 활성 상태의 관리자 계정과 올바른 email·password가 주어졌을 때, **When** 로그인 요청을 보내면, **Then** 인증 Session이 생성되고 Session Cookie와 함께 관리자 최소 정보(JSON)가 반환된다.
2. **Given** 로그인에 성공했을 때, **When** 응답을 확인하면, **Then** password는 어떤 형태로도 응답에 포함되지 않는다.
3. **Given** 로그인 요청 본문이 JSON 형식이 아니거나 필수 필드가 누락되었을 때, **When** 로그인 요청을 보내면, **Then** 유효성 검사 실패 응답이 반환되고 Session은 생성되지 않는다.

---

### User Story 2 - 로그인 실패 처리 (Priority: P1)

존재하지 않는 계정, 잘못된 비밀번호, 비활성화된 계정으로 로그인을 시도하면 인증에 실패한다. 실패 시에는 어떤 경우든 Session이 생성되지 않으며, 계정 존재 여부나 실패 사유를 구분할 수 있는 정보를 노출하지 않는 동일한 실패 응답을 반환한다.

**Why this priority**: 로그인 성공 경로만큼 실패 경로의 일관성이 보안 경계의 핵심이다. 계정 존재 여부가 새어나가면 계정 탐색 공격에 노출된다.

**Independent Test**: 존재하지 않는 email, 존재하지만 틀린 password, 비활성화된 계정 세 가지로 각각 로그인을 시도해 동일한 실패 응답 구조와 Session 미생성을 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 존재하지 않는 email로 로그인을 시도할 때, **When** 로그인 요청을 보내면, **Then** 인증 실패 응답이 반환되고 Session은 생성되지 않는다.
2. **Given** 존재하는 email에 잘못된 password로 로그인을 시도할 때, **When** 로그인 요청을 보내면, **Then** 1번과 동일한 형태의 인증 실패 응답이 반환된다.
3. **Given** 비활성화된 관리자 계정으로 올바른 password를 입력했을 때, **When** 로그인 요청을 보내면, **Then** 인증 실패 응답이 반환되고 Session은 생성되지 않는다.

---

### User Story 3 - Session 기반 인증 유지 (Priority: P1)

로그인에 성공한 관리자는 이후 요청마다 email과 password를 다시 전달하지 않는다. 서버는 Session Cookie를 기준으로 인증 상태를 확인하며, 유효한 Session이 있으면 보호된 Admin API에 접근할 수 있다.

**Why this priority**: 매 요청마다 자격 증명을 재전달하지 않는 것이 Session 인증의 존재 이유이며, 이후 모든 Admin API 보호가 이 흐름 위에서 동작한다.

**Independent Test**: 로그인 후 발급된 Session Cookie만으로 별도의 Admin API를 연속 호출해 인증 상태가 유지되는지 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 로그인 성공으로 발급된 Session Cookie가 있을 때, **When** 해당 Cookie로 보호된 Admin API를 호출하면, **Then** password 재전달 없이 요청이 정상 처리된다.
2. **Given** Session Cookie 없이 요청할 때, **When** 보호된 Admin API를 호출하면, **Then** 요청이 거부된다.
3. **Given** 로그인 시 발급된 Session이 기존 익명 Session을 재사용하지 않았는지 확인할 때, **When** 로그인 전후 Session 식별자를 비교하면, **Then** 로그인 성공 시점에 새로운 Session 식별자로 교체되어 있다.

---

### User Story 4 - 현재 관리자 정보 조회 (Priority: P2)

클라이언트는 현재 Session이 유효한 관리자 인증 상태인지 확인할 수 있다. 인증된 경우 관리자 식별자, email, role 등 최소 정보를 반환하며, 비밀번호나 내부 보안 정보는 포함하지 않는다. 이 기능은 Frontend가 새로고침 이후 로그인 상태를 복원하는 데 사용된다.

**Why this priority**: 로그인·로그아웃 흐름이 갖춰진 뒤 Frontend가 현재 인증 상태를 확인할 수단이 필요하지만, 로그인 자체보다는 부가적인 조회 기능이다.

**Independent Test**: 로그인 상태와 비로그인 상태 각각에서 현재 관리자 조회 API를 호출해 응답 차이를 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 유효한 Session으로 인증된 상태일 때, **When** 현재 관리자 조회 요청을 보내면, **Then** 관리자 식별자·email·role을 담은 200 응답이 반환된다.
2. **Given** 인증되지 않은 상태일 때, **When** 현재 관리자 조회 요청을 보내면, **Then** 인증 실패 응답이 반환된다.
3. **Given** 현재 관리자 조회에 성공했을 때, **When** 응답 내용을 확인하면, **Then** password 및 내부 보안 정보는 포함되지 않는다.

---

### User Story 5 - 관리자 로그아웃 (Priority: P2)

로그인된 관리자는 REST API를 통해 로그아웃할 수 있다. 로그아웃하면 현재 인증 Session이 무효화되며, 이후 동일한 Session Cookie로는 보호된 Admin API에 접근할 수 없다.

**Why this priority**: Session 생성과 유지 흐름이 확립된 이후 이를 명시적으로 종료하는 대응 기능이다.

**Independent Test**: 로그인 후 로그아웃을 호출하고, 동일한 Session Cookie로 보호된 Admin API를 다시 호출했을 때 거부되는지 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 로그인된 상태일 때, **When** 로그아웃 요청을 보내면, **Then** JSON 응답과 함께 현재 Session이 무효화된다.
2. **Given** 로그아웃이 완료된 Session Cookie가 있을 때, **When** 해당 Cookie로 보호된 Admin API를 호출하면, **Then** 요청이 거부된다.
3. **Given** 이미 인증되지 않은 상태일 때, **When** 로그아웃 요청을 보내면, **Then** 인증 실패 응답이 반환된다.

---

### User Story 6 - Admin API 접근 보호 (Priority: P1)

Category, Media, Work 관리 기능(생성·수정·삭제·공개 여부 변경·관리자 조회 등)은 인증된 ADMIN 권한 사용자만 실행할 수 있다. 인증되지 않은 요청은 해당 기능을 실행하지 못하며, 인증되었더라도 필요한 권한이 없으면 실행할 수 없다.

**Why this priority**: 기존 Phase에서 구현한 관리 기능을 실제로 보호하는 것이 이번 Phase의 핵심 목적이다. 로그인·Session 흐름은 이 보호를 가능하게 하는 수단이다.

**Independent Test**: 비로그인 상태와 로그인된 ADMIN 상태 각각으로 기존 Category/Media/Work 관리 API를 호출해 거부/허용 여부를 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 인증되지 않은 사용자가, **When** Category/Media/Work 관리 API 중 하나를 호출하면, **Then** 요청이 실행되지 않고 401 응답이 반환된다.
2. **Given** 유효한 Session으로 인증된 ADMIN 사용자가, **When** 동일한 관리 API를 호출하면, **Then** 기존 Phase에서 구현된 비즈니스 로직이 정상 실행된다.
3. **Given** 인증되었지만 ADMIN 권한이 없는 사용자가, **When** 관리 API를 호출하면, **Then** 요청이 실행되지 않고 403 응답이 반환된다.

---

### User Story 7 - Public API 공개 접근 유지 (Priority: P1)

공개 Category 조회, 공개 Work 목록·상세 조회, 공개 Category별 Work 조회는 관리자 로그인 여부와 관계없이 인증 없이 접근할 수 있다.

**Why this priority**: Admin 인증 도입이 기존 Public 영역의 동작을 변경하지 않는다는 것을 보장해야, 인증 도입이 회귀(regression)를 일으키지 않았다고 확신할 수 있다.

**Independent Test**: 인증 정보 없이 Public 조회 API들을 호출해 기존과 동일하게 정상 응답하는지 확인할 수 있다.

**Acceptance Scenarios**:

1. **Given** 인증 정보가 전혀 없을 때, **When** 공개 Work 목록·상세 조회 API를 호출하면, **Then** 정상적으로 조회 결과가 반환된다.
2. **Given** 인증 정보가 전혀 없을 때, **When** 공개 Category 조회 API를 호출하면, **Then** 정상적으로 조회 결과가 반환된다.
3. **Given** 관리자가 로그인되어 있는 상태와 아닌 상태 각각에서, **When** 동일한 Public 조회 API를 호출하면, **Then** 두 경우 모두 동일한 공개 정책에 따른 응답을 받는다.

---

### Edge Cases

- 인증되지 않은 요청이 보호된 Admin API에 접근하면 로그인 페이지로 Redirect하지 않고 401 JSON 응답을 반환해야 한다.
- 인증은 되었으나 ADMIN 권한이 없는 요청은 401이 아닌 403으로 명확히 구분되어야 한다.
- 로그인 요청 자체는 인증이 필요하지 않지만, 이미 인증된 Session으로 다시 로그인 요청을 보내는 경우도 정상 처리(재인증)되어야 한다.
- Session이 만료되었거나 서버 재시작 등으로 유효하지 않게 된 경우, 해당 Session은 인증된 것으로 취급되지 않아야 한다.
- 상태를 변경하지 않는 Public 조회 요청은 CSRF 보호 대상에서 제외되고, Admin의 상태 변경 요청(POST/PUT/PATCH/DELETE)만 CSRF 보호 대상이 되어야 한다.
- 신뢰하지 않는 Origin에서 Session Cookie를 포함한 인증 요청을 보내는 경우, Credential 기반 요청이 허용되지 않아야 한다.
- 로그인 실패 응답만으로는 요청한 email이 실제 존재하는 계정인지 추론할 수 없어야 한다.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 관리자는 email과 password를 JSON 형식으로 전달해 로그인할 수 있어야 한다.
- **FR-002**: 로그인 성공 응답은 JSON 형식이어야 하며, Redirect나 HTML을 반환하지 않아야 한다.
- **FR-003**: 로그인 성공 시 새로운 인증 Session이 생성되고, 클라이언트는 Session Cookie를 통해 인증 상태를 유지할 수 있어야 한다.
- **FR-004**: 로그인 성공 시 Session Fixation 방어가 적용되어, 로그인 이전의 익명 Session을 그대로 인증 Session으로 신뢰하지 않아야 한다.
- **FR-005**: 존재하지 않는 계정, 잘못된 비밀번호, 비활성화된 계정으로는 로그인에 실패해야 하며, 세 경우 모두 동일한 형태의 실패 응답을 반환해야 한다.
- **FR-006**: 로그인 실패 시 인증 Session이 생성되지 않아야 한다.
- **FR-007**: 관리자는 현재 Session의 인증 상태 및 자신의 최소 정보(식별자, email, role)를 조회할 수 있어야 하며, 응답에는 password나 내부 보안 정보가 포함되지 않아야 한다.
- **FR-008**: 관리자는 REST API를 통해 로그아웃할 수 있어야 하며, 로그아웃 성공 시 현재 Session은 무효화되고 이후 해당 Session으로는 인증된 것으로 처리되지 않아야 한다.
- **FR-009**: 인증되지 않은 요청이 Admin 관리 기능(Category/Media/Work의 생성·수정·삭제·공개 여부 변경·관리자 조회)을 호출하면 실행되지 않고 401 응답을 반환해야 한다.
- **FR-010**: 인증되었으나 ADMIN 권한이 없는 요청이 Admin 관리 기능을 호출하면 실행되지 않고 403 응답을 반환해야 한다.
- **FR-011**: 인증 실패와 인가 실패는 모두 JSON 응답으로 반환되어야 하며, 로그인 페이지로 Redirect되지 않아야 한다.
- **FR-012**: Public 영역의 조회 API(공개 Category 조회, 공개 Work 목록·상세 조회, 공개 Category별 Work 조회)는 관리자 로그인 여부와 무관하게 인증 없이 접근 가능해야 한다.
- **FR-013**: 관리자 비밀번호는 원문으로 저장되지 않아야 하며, 안전한 단방향 해시 형태로 저장되어야 한다.
- **FR-014**: 비밀번호 원문은 API 응답, 로그, 예외 메시지 어디에도 노출되지 않아야 한다.
- **FR-015**: Admin의 상태 변경 요청(POST/PUT/PATCH/DELETE)은 CSRF 공격으로부터 보호되어야 하며, Frontend가 CSRF Token을 획득하고 이후 요청에 전달할 수 있는 구조를 제공해야 한다.
- **FR-016**: Session Cookie 기반 인증 요청을 허용할 Frontend Origin은 명시적으로 관리되어야 하며, Credential을 포함하는 요청에서 임의의 모든 Origin을 허용하지 않아야 한다.
- **FR-017**: 초기 관리자 계정은 회원가입 API가 아닌 관리 가능한 초기화 절차(애플리케이션 초기화, 운영 설정 등)로 준비할 수 있어야 한다.
- **FR-018**: 인증 도입을 위해 Work, Category, Media Domain은 Spring Security API에 직접 의존하지 않아야 한다.

### Key Entities

- **Admin**: 관리자 인증 계정. 관리자 식별자, 로그인 식별자(email), 비밀번호 해시, role(ADMIN), 활성 상태, 생성 시각, 수정 시각을 가진다. 비밀번호 원문은 보관하지 않는다.
- **AdminRole**: 관리자 권한 구분. MVP에서는 `ADMIN` 단일 값으로 시작한다.
- **AuthenticationSession**: 로그인 성공 시 서버에 생성되는 인증 상태. Session Cookie로 클라이언트와 식별자를 교환하며, 로그아웃 또는 만료 시 무효화된다.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 올바른 자격 증명으로 로그인한 관리자는 이후 요청에서 비밀번호 재전달 없이 보호된 Admin API에 100% 접근할 수 있다.
- **SC-002**: 잘못된 자격 증명, 존재하지 않는 계정, 비활성화된 계정 세 가지 실패 시나리오 모두 동일한 실패 응답 구조를 반환해 계정 존재 여부를 추론할 수 없다.
- **SC-003**: 인증되지 않은 요청의 Admin 관리 기능 호출은 100% 401로 차단되고, 인증되었지만 권한 없는 요청은 100% 403으로 차단된다.
- **SC-004**: 로그아웃 이후 기존 Session Cookie로의 Admin API 요청은 100% 차단된다.
- **SC-005**: Public 조회 API는 인증 도입 전후로 동일하게 인증 없이 100% 접근 가능하다.
- **SC-006**: 비밀번호 원문은 DB, 응답, 로그, 예외 메시지 어디에서도 발견되지 않는다.
- **SC-007**: 신뢰 목록에 없는 Origin에서 발생한 Credential 포함 요청은 100% 차단된다.

## Assumptions

- 인증 방식은 Spring Security 기반 Session Authentication이며, JWT·OAuth2·소셜 로그인은 이번 Phase 범위 밖이다.
- 관리자 권한은 MVP 단계에서 `ADMIN` 단일 role만 존재하며, 세분화된 RBAC는 이후 확장 대상이다.
- 일반 방문자를 위한 회원가입, 이메일 인증, 비밀번호 찾기/재설정 기능은 제공하지 않는다.
- 초기 관리자 계정 준비 방식(애플리케이션 초기화, 데이터 초기화 절차 등)의 구체적인 구현 방식은 Plan 단계에서 결정한다.
- Session Cookie의 HttpOnly·Secure·SameSite 등 구체적인 운영값과 CSRF Token 전달 방식은 Plan 단계에서 실제 배포 도메인 구조에 맞춰 결정한다.
- 허용 Origin 목록의 구체적인 도메인은 환경 설정 단계에서 결정하며, 개발·운영 환경별로 다르게 관리될 수 있다.
- Redis 기반 분산 Session, Remember-Me, MFA, 로그인 시도 Rate Limit, 계정 잠금 고도화, 관리자 활동 감사 로그는 이번 Phase 범위 밖이다.
- Integrated Chat System(AI 챗봇, 실시간 상담)의 인증 정책은 이번 Phase에서 확정하지 않는다.
- 기존 Category, Media, Work의 비즈니스 로직 자체는 변경하지 않으며, 이번 Phase는 그 앞에 접근 제어 경계만 추가한다.
