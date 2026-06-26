# Feature Specification: Category Management

**Feature Branch**: `feature/4-category-management`
**Created**: 2026-06-16
**Status**: Draft
**Input**: User description: "PHASE_02_category_management"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 카테고리 생성 (Priority: P1)

관리자는 작업물을 분류하기 위한 카테고리를 생성한다. 이름, 슬러그, 노출 순서, 공개 여부를 입력하면 카테고리가 저장된다.

**Why this priority**: 카테고리가 존재해야 수정, 삭제, 조회가 의미 있다. 이후 모든 스토리의 전제 조건이다.

**Independent Test**: `POST /api/admin/categories`로 카테고리를 생성하고 201 응답과 생성된 데이터를 확인한다. 중복 슬러그로 재요청 시 409 응답을 확인한다.

**Acceptance Scenarios**:

1. **Given** 유효한 이름, 슬러그, 노출 순서, 공개 여부가 주어졌을 때, **When** 관리자가 카테고리 생성을 요청하면, **Then** 201 응답과 생성된 카테고리 정보가 반환된다.
2. **Given** 이미 존재하는 슬러그가 주어졌을 때, **When** 관리자가 카테고리 생성을 요청하면, **Then** 409 응답과 `CATEGORY_SLUG_DUPLICATE` 에러 코드가 반환된다.
3. **Given** 이름 또는 슬러그가 비어 있을 때, **When** 관리자가 카테고리 생성을 요청하면, **Then** 400 응답과 필드 오류 정보가 반환된다.

---

### User Story 2 - 카테고리 수정 (Priority: P2)

관리자는 기존 카테고리의 이름, 슬러그, 노출 순서, 공개 여부를 수정할 수 있다.

**Why this priority**: 생성 후 카테고리 정보를 변경할 수 있어야 운영이 가능하다.

**Independent Test**: 생성된 카테고리를 `PUT /api/admin/categories/{id}`로 수정하고 200 응답을 확인한다. 자기 자신의 슬러그로 수정 시 허용, 타 카테고리 슬러그로 수정 시 409를 확인한다.

**Acceptance Scenarios**:

1. **Given** 존재하는 카테고리와 유효한 수정 데이터가 주어졌을 때, **When** 관리자가 수정을 요청하면, **Then** 200 응답과 수정된 카테고리 정보가 반환된다.
2. **Given** 수정 슬러그가 자기 자신의 기존 슬러그와 동일할 때, **When** 관리자가 수정을 요청하면, **Then** 정상 처리된다.
3. **Given** 수정 슬러그가 다른 카테고리의 슬러그와 중복될 때, **When** 관리자가 수정을 요청하면, **Then** 409 응답과 `CATEGORY_SLUG_DUPLICATE` 에러 코드가 반환된다.
4. **Given** 존재하지 않는 카테고리 ID가 주어졌을 때, **When** 관리자가 수정을 요청하면, **Then** 404 응답과 `CATEGORY_NOT_FOUND` 에러 코드가 반환된다.

---

### User Story 3 - 카테고리 삭제 (Priority: P2)

관리자는 더 이상 사용하지 않는 카테고리를 삭제할 수 있다. 작업물이 연결된 카테고리는 삭제할 수 없다.

**Why this priority**: 생성/수정과 함께 기본 CRUD를 완성한다. Work 연결 검사 확장 지점을 이 단계에서 준비한다.

**Independent Test**: 생성된 카테고리를 `DELETE /api/admin/categories/{id}`로 삭제하고 204를 확인한다. 존재하지 않는 ID 삭제 시 404를 확인한다.

**Acceptance Scenarios**:

1. **Given** 존재하는 카테고리가 주어졌을 때, **When** 관리자가 삭제를 요청하면, **Then** 204 응답이 반환된다.
2. **Given** 존재하지 않는 카테고리 ID가 주어졌을 때, **When** 관리자가 삭제를 요청하면, **Then** 404 응답과 `CATEGORY_NOT_FOUND` 에러 코드가 반환된다.

---

### User Story 4 - 관리자 카테고리 목록 조회 (Priority: P3)

관리자는 공개 여부와 관계없이 모든 카테고리를 노출 순서 기준으로 조회할 수 있다.

**Why this priority**: CRUD 완성 후 관리자가 전체 카테고리 현황을 파악할 수 있어야 한다.

**Independent Test**: `GET /api/admin/categories`로 목록을 조회하고 공개/비공개 카테고리가 모두 포함되며 `displayOrder` ASC, `createdAt` ASC 정렬을 확인한다.

**Acceptance Scenarios**:

1. **Given** 공개/비공개 카테고리가 존재할 때, **When** 관리자가 목록을 조회하면, **Then** 전체 카테고리가 `displayOrder` ASC, `createdAt` ASC 순으로 반환된다.
2. **Given** 카테고리가 없을 때, **When** 관리자가 목록을 조회하면, **Then** 빈 배열이 반환된다.

---

### User Story 5 - 공개 카테고리 목록 조회 (Priority: P3)

공개 사이트 방문자는 공개 상태인 카테고리만 노출 순서 기준으로 조회할 수 있다.

**Why this priority**: 공개 사이트에서 필터 기준으로 사용할 카테고리 목록을 제공한다.

**Independent Test**: `GET /api/public/categories`로 목록을 조회하고 비공개 카테고리가 포함되지 않는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 공개/비공개 카테고리가 혼재할 때, **When** 방문자가 공개 카테고리 목록을 조회하면, **Then** `visible = true`인 카테고리만 `displayOrder` ASC, `createdAt` ASC 순으로 반환된다.
2. **Given** 공개 카테고리가 없을 때, **When** 방문자가 목록을 조회하면, **Then** 빈 배열이 반환된다.

---

### Edge Cases

- 슬러그 중복 검사는 대소문자를 구분하는가? (구분함 — `Branding`과 `branding`은 다른 슬러그)
- `displayOrder`가 동일한 카테고리가 여러 개일 때 정렬 순서는? (`createdAt` ASC로 2차 정렬)
- 존재하지 않는 카테고리를 수정/삭제하면? (`CATEGORY_NOT_FOUND` 404 반환)
- 이름이 공백 문자열만 포함될 때 검증 실패하는가? (`@NotBlank`로 처리, 공백만 있어도 실패)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 관리자는 이름, 슬러그, 노출 순서, 공개 여부를 입력해 카테고리를 생성할 수 있어야 한다.
- **FR-002**: 카테고리 슬러그는 전체 카테고리 중 중복될 수 없어야 한다.
- **FR-003**: 관리자는 기존 카테고리의 이름, 슬러그, 노출 순서, 공개 여부를 수정할 수 있어야 한다.
- **FR-004**: 카테고리 수정 시 자기 자신의 기존 슬러그는 중복으로 간주하지 않아야 한다.
- **FR-005**: 관리자는 카테고리를 삭제할 수 있어야 한다.
- **FR-006**: 존재하지 않는 카테고리를 조회, 수정, 삭제하려는 경우 `CATEGORY_NOT_FOUND` 에러 응답을 반환해야 한다.
- **FR-007**: 관리자는 공개 여부와 관계없이 모든 카테고리를 조회할 수 있어야 한다.
- **FR-008**: 공개 사이트에서는 `visible = true`인 카테고리만 조회할 수 있어야 한다.
- **FR-009**: 카테고리 목록은 `displayOrder` ASC, `createdAt` ASC 순으로 정렬되어야 한다.
- **FR-010**: 카테고리 이름과 슬러그는 비어 있을 수 없다.
- **FR-011**: 카테고리 삭제 시 작업물 연결 여부를 work application port로 확인하고, 연결된 작업물이 있으면 삭제를 거부해야 한다.
- **FR-012**: category 영역은 work infrastructure/domain 구현체에 직접 의존하지 않아야 하며, 필요한 Work 참조 확인은 work application port를 통해 수행할 수 있다.

### Key Entities

- **Category**: 작업물 분류 단위 — 이름, 슬러그(unique), 노출 순서, 공개 여부, 생성/수정 시각 포함

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 관리자가 카테고리 생성/수정/삭제/조회 각 작업을 단일 요청으로 완료할 수 있다.
- **SC-002**: 중복 슬러그로 요청 시 100% `CATEGORY_SLUG_DUPLICATE` 에러 응답이 반환된다.
- **SC-003**: 공개 카테고리 목록에서 비공개 카테고리가 0건 포함된다.
- **SC-004**: 카테고리 목록이 항상 `displayOrder` ASC, `createdAt` ASC 기준으로 반환된다.
- **SC-005**: category 패키지가 work infrastructure/domain 구현체를 직접 import하지 않는다.

## Assumptions

- 관리자 인증은 이번 단계 범위 밖이며, Admin API는 임시로 인증 없이 접근 가능하다. (Auth 단계에서 `.authenticated()`로 전환)
- 슬러그는 대소문자를 구분한다.
- 카테고리 목록은 페이지네이션 없이 전체 반환한다. (카테고리 수가 제한적이라는 전제)
- Work 연결 검사는 category application delete flow에서 work application port를 호출해 수행한다.
