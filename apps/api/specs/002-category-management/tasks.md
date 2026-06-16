# Tasks: Category Management

**Input**: Design documents from `/specs/002-category-management/`
**Prerequisites**: plan.md ✅, spec.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: User Story 기준으로 태스크를 구성하여 각 스토리를 독립적으로 구현·테스트할 수 있도록 한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1~US5)
- 모든 경로는 `apps/api/src/` 기준

---

## Phase 1: Setup

**Purpose**: shared 기반 수정 및 feature 브랜치 준비

- [ ] T001 `shared/exception/ErrorCode.java`에 `CATEGORY_NOT_FOUND`, `CATEGORY_SLUG_DUPLICATE`, `CATEGORY_HAS_WORKS` 에러 코드 추가
- [ ] T002 `shared/config/SecurityConfig.java`의 `/api/admin/**` 규칙을 임시 `.permitAll()`로 변경 (Auth 단계 전까지)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story가 공통으로 의존하는 Category 도메인 핵심 구조

**⚠️ CRITICAL**: 이 Phase 완료 전에는 어떤 User Story 작업도 시작하지 않는다.

- [ ] T003 [P] `category/domain/Category.java` 엔티티 구현 (`BaseTimeEntity` 상속, `name`/`slug`/`displayOrder`/`visible` 필드, `protected` 기본 생성자, `Category.create()` static factory, `category.update()` domain method)
- [ ] T004 [P] `category/domain/CategoryRepository.java` 포트 인터페이스 정의 (`save`, `findById`, `findBySlug`, `existsBySlug`, `existsBySlugAndIdNot`, `findAllByOrderByDisplayOrderAscCreatedAtAsc`, `findAllByVisibleTrueOrderByDisplayOrderAscCreatedAtAsc`, `delete`)
- [ ] T005 [P] `category/domain/CategoryDeletionPolicy.java` 인터페이스 정의 (`void validate(Category category)` — 위반 시 `BusinessException` 던짐)
- [ ] T006 `category/infrastructure/JpaCategoryRepository.java` Spring Data JPA 구현체 (`CategoryRepository` 구현, `@Repository`)
- [ ] T007 `category/infrastructure/DefaultCategoryDeletionPolicy.java` 기본 구현 (현재 단계는 아무 검사 없이 통과)

**Checkpoint**: Category 도메인 기반 준비 완료 → US 구현 시작 가능

---

## Phase 3: User Story 1 — 카테고리 생성 (Priority: P1) 🎯 MVP

**Goal**: 관리자가 카테고리를 생성할 수 있다. 슬러그 중복 시 명확한 실패 응답을 받는다.

**Independent Test**: `POST /api/admin/categories`로 카테고리 생성 → 201 확인. 동일 슬러그 재요청 → 409 확인. 빈 이름으로 요청 → 400 확인.

- [ ] T008 [US1] `category/application/CategoryCommandService.java` 생성 메서드 구현 (`createCategory` — 슬러그 중복 검사 → `Category.create()` → `save`)
- [ ] T009 [P] [US1] `category/presentation/dto/CreateCategoryRequest.java` 구현 (`@NotBlank name`, `@NotBlank slug`, `displayOrder`, `visible`)
- [ ] T010 [P] [US1] `category/presentation/dto/CategoryResponse.java` 구현 (`id`, `name`, `slug`, `displayOrder`, `visible`, `createdAt`, `updatedAt`, `CategoryResponse.from(Category)` factory)
- [ ] T011 [US1] `category/presentation/AdminCategoryController.java` 생성 엔드포인트 구현 (`POST /api/admin/categories` → 201 반환)
- [ ] T012 [US1] `test/.../category/application/CategoryCommandServiceTest.java` — 생성 흐름, 슬러그 중복 예외 검증 (Mockito)
- [ ] T013 [US1] `test/.../category/presentation/AdminCategoryControllerTest.java` — 생성 HTTP 계약, 요청 검증, 에러 응답 (`@WebMvcTest`)

**Checkpoint**: US1 완료 — 카테고리 생성 기능 독립 검증 가능

---

## Phase 4: User Story 2 — 카테고리 수정 (Priority: P2)

**Goal**: 관리자가 기존 카테고리를 수정할 수 있다. 자기 자신 슬러그는 허용, 타 카테고리 슬러그 중복 시 409.

**Independent Test**: 생성된 카테고리를 `PUT /api/admin/categories/{id}`로 수정 → 200 확인. 자기 슬러그 유지 수정 → 200 확인. 타 카테고리 슬러그 사용 → 409 확인. 미존재 ID → 404 확인.

- [ ] T014 [US2] `CategoryCommandService.java`에 `updateCategory` 메서드 추가 (미존재 → `CATEGORY_NOT_FOUND`, 타 슬러그 중복 → `CATEGORY_SLUG_DUPLICATE`, `category.update()` 호출)
- [ ] T015 [P] [US2] `category/presentation/dto/UpdateCategoryRequest.java` 구현 (`@NotBlank name`, `@NotBlank slug`, `displayOrder`, `visible`)
- [ ] T016 [US2] `AdminCategoryController.java`에 수정 엔드포인트 추가 (`PUT /api/admin/categories/{id}` → 200 반환)
- [ ] T017 [US2] `CategoryCommandServiceTest.java`에 수정 흐름 테스트 추가 (자기 슬러그 허용, 타 슬러그 중복 예외, 미존재 예외)
- [ ] T018 [US2] `AdminCategoryControllerTest.java`에 수정 HTTP 계약 테스트 추가

**Checkpoint**: US2 완료 — 카테고리 수정 기능 독립 검증 가능

---

## Phase 5: User Story 3 — 카테고리 삭제 (Priority: P2)

**Goal**: 관리자가 카테고리를 삭제할 수 있다. 미존재 시 404. `CategoryDeletionPolicy`로 확장 가능.

**Independent Test**: 생성된 카테고리를 `DELETE /api/admin/categories/{id}`로 삭제 → 204 확인. 미존재 ID → 404 확인.

- [ ] T019 [US3] `CategoryCommandService.java`에 `deleteCategory` 메서드 추가 (미존재 → `CATEGORY_NOT_FOUND`, `CategoryDeletionPolicy` 순회 후 `delete`)
- [ ] T020 [US3] `AdminCategoryController.java`에 삭제 엔드포인트 추가 (`DELETE /api/admin/categories/{id}` → 204 반환)
- [ ] T021 [US3] `CategoryCommandServiceTest.java`에 삭제 흐름 테스트 추가 (정상 삭제, 미존재 예외)
- [ ] T022 [US3] `AdminCategoryControllerTest.java`에 삭제 HTTP 계약 테스트 추가

**Checkpoint**: US3 완료 — 카테고리 삭제 기능 독립 검증 가능

---

## Phase 6: User Story 4 — 관리자 카테고리 목록 조회 (Priority: P3)

**Goal**: 관리자가 공개/비공개 전체 카테고리를 `displayOrder` ASC, `createdAt` ASC 순으로 조회한다.

**Independent Test**: `GET /api/admin/categories`로 목록 조회 → 공개/비공개 모두 포함, 정렬 순서 확인.

- [ ] T023 [US4] `category/application/CategoryQueryService.java` 구현 (`findAllForAdmin` — `displayOrder` ASC, `createdAt` ASC 정렬)
- [ ] T024 [US4] `AdminCategoryController.java`에 목록 조회 엔드포인트 추가 (`GET /api/admin/categories` → `List<CategoryResponse>` 반환)
- [ ] T025 [US4] `test/.../category/application/CategoryQueryServiceTest.java` — 전체 목록 반환, 정렬 검증 (Mockito)
- [ ] T026 [US4] `AdminCategoryControllerTest.java`에 목록 조회 HTTP 계약 테스트 추가

**Checkpoint**: US4 완료 — 관리자 카테고리 목록 조회 독립 검증 가능

---

## Phase 7: User Story 5 — 공개 카테고리 목록 조회 (Priority: P3)

**Goal**: 방문자가 `visible = true`인 카테고리만 조회한다. 비공개 카테고리는 응답에 포함되지 않는다.

**Independent Test**: `GET /api/public/categories`로 목록 조회 → 비공개 카테고리 미포함 확인.

- [ ] T027 [US5] `CategoryQueryService.java`에 `findAllVisible` 메서드 추가 (`visible = true`인 카테고리만, `displayOrder` ASC, `createdAt` ASC 정렬)
- [ ] T028 [US5] `category/presentation/PublicCategoryController.java` 구현 (`GET /api/public/categories` → `List<CategoryResponse>` 반환)
- [ ] T029 [US5] `CategoryQueryServiceTest.java`에 공개 필터 테스트 추가
- [ ] T030 [US5] `test/.../category/presentation/PublicCategoryControllerTest.java` — 공개 목록 HTTP 계약, 비공개 필터 확인 (`@WebMvcTest`)

**Checkpoint**: US5 완료 — 공개 카테고리 목록 조회 독립 검증 가능

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 도메인 경계 검증 및 전체 통합 확인

- [ ] T031 [P] `test/.../category/domain/CategoryTest.java` — `Category.create()`, `update()` 동작, 필드 초기값 단위 테스트
- [ ] T032 [P] `test/.../category/infrastructure/JpaCategoryRepositoryTest.java` — 슬러그 유니크 제약, 정렬 순서 (`@DataJpaTest` + Testcontainers PostgreSQL)
- [ ] T033 category 패키지의 work/media/chat/ai 의존성 없음 확인
- [ ] T034 전체 테스트 실행 — `./gradlew test` 전체 통과 확인

---

## Dependencies & Execution Order

### Phase 의존 관계

- **Phase 1 (Setup)**: 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 후 — **모든 US를 블로킹**
- **Phase 3~7 (US1~US5)**: Phase 2 완료 후 순차 진행 (US1 → US2 → US3 → US4 → US5)
- **Phase 8 (Polish)**: 모든 US 완료 후

### User Story 의존 관계

- **US1**: Phase 2 완료 후 시작 — 독립 구현 가능 (`CategoryCommandService` 신규)
- **US2**: US1 완료 후 시작 — `CategoryCommandService`에 메서드 추가
- **US3**: US2 완료 후 시작 — `CategoryCommandService`에 메서드 추가
- **US4**: Phase 2 완료 후 시작 — `CategoryQueryService` 신규 (US1~US3과 병렬 가능)
- **US5**: US4 완료 후 시작 — `CategoryQueryService`에 메서드 추가

### Parallel Opportunities

```bash
# Phase 2 병렬 실행 가능
T003 Category 엔티티, T004 CategoryRepository, T005 CategoryDeletionPolicy

# Phase 3 내부 병렬 가능
T009 CreateCategoryRequest, T010 CategoryResponse (서로 다른 파일)

# Phase 4 내부 병렬 가능
T015 UpdateCategoryRequest

# Phase 8 병렬 가능
T031 CategoryTest, T032 JpaCategoryRepositoryTest
```

---

## Implementation Strategy

### MVP (US1만 구현)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료
3. Phase 3: US1 완료 (T008~T013)
4. **검증**: 카테고리 생성/슬러그 중복 응답 동작 확인
5. 나머지 US는 순차 추가

### 전체 구현 순서

1. Setup → Foundational
2. US1 (생성) → US2 (수정) → US3 (삭제) — CommandService 완성
3. US4 (관리자 목록) → US5 (공개 목록) — QueryService 완성
4. Polish

---

## Notes

- `[P]` 태스크는 다른 파일을 수정하므로 병렬 실행 가능
- `[USN]` 레이블로 각 태스크가 어느 스토리에 속하는지 추적 가능
- 각 Phase Checkpoint에서 독립 검증 후 다음 Phase 진행
- `DefaultCategoryDeletionPolicy`는 현재 단계에서 항상 허용, Work 단계에서 `WorkLinkedCategoryDeletionPolicy`로 교체
- Admin API의 `.permitAll()` 임시 설정은 Auth 단계 완료 후 `.authenticated()`로 되돌린다
