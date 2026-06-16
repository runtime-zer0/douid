# Tasks: Shared Foundation

**Input**: Design documents from `/specs/001-shared-foundation/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: User Story 기준으로 태스크를 구성하여 각 스토리를 독립적으로 구현·테스트할 수 있도록 한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1~US4)
- 모든 경로는 `apps/api/src/` 기준

---

## Phase 1: Setup

**Purpose**: 공통 기반 패키지 디렉토리 구조 생성 및 JPA Auditing 활성화

- [x] T001 `@EnableJpaAuditing` 추가 — `apps/api/src/main/java/kr/douid/brand/shared/config/JpaConfig.java` (별도 설정 클래스로 분리)
- [x] T002 shared 패키지 하위 디렉토리 생성 — `apps/api/src/main/java/kr/douid/brand/shared/{response,exception,pagination,config,entity}/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 모든 User Story가 공통으로 의존하는 핵심 구조 — 이 Phase가 완료되어야 US 구현 시작 가능

**⚠️ CRITICAL**: 이 Phase 완료 전에는 어떤 User Story 작업도 시작하지 않는다.

- [x] T003 [P] `ErrorCode` enum 구현 — `apps/api/src/main/java/kr/douid/brand/shared/exception/ErrorCode.java`
- [x] T004 [P] `BusinessException` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/exception/BusinessException.java`
- [x] T005 [P] `BaseTimeEntity` abstract 클래스 구현 — `apps/api/src/main/java/kr/douid/brand/shared/entity/BaseTimeEntity.java`

**Checkpoint**: ErrorCode, BusinessException, BaseTimeEntity 준비 완료 → US 구현 시작 가능

---

## Phase 3: User Story 1 — 일관된 API 응답 수신 (Priority: P1) 🎯 MVP

**Goal**: 모든 API가 동일한 성공/실패 응답 구조를 반환한다.

**Independent Test**: `curl http://localhost:8080/api/public/health` 로 `{"success":true,"data":...}` 구조 확인. 존재하지 않는 경로 요청 시 `{"success":false,"error":{...}}` 구조 확인.

- [x] T006 [P] [US1] `ApiResponse<T>` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/response/ApiResponse.java`
- [x] T007 [P] [US1] `ErrorResponse` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/response/ErrorResponse.java`
- [x] T008 [US1] `GlobalExceptionHandler` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/exception/GlobalExceptionHandler.java`
- [x] T009 [US1] `GlobalExceptionHandlerTest` 작성 — `apps/api/src/test/java/kr/douid/brand/shared/exception/GlobalExceptionHandlerTest.java` (standaloneSetup 방식)

**Checkpoint**: US1 완료 — 모든 API 응답 구조 일관성 보장

---

## Phase 4: User Story 2 — 비즈니스/시스템 예외 구분 (Priority: P2)

**Goal**: 클라이언트가 비즈니스 예외(명확한 에러 코드)와 시스템 예외(일반 오류 메시지)를 구분해서 받는다.

**Independent Test**: `BusinessException(ErrorCode.NOT_FOUND)` 발생 시 404 + `NOT_FOUND` 코드 반환 확인. 의도적 NPE 발생 시 500 + 스택 트레이스 미포함 확인.

> US1(T006~T009)의 GlobalExceptionHandler가 이미 두 예외를 구분 처리하므로, 이 Phase는 검증 테스트와 이후 도메인 확장 가이드 확인에 집중한다.

- [x] T010 [US2] `BusinessExceptionTest` 작성 — `apps/api/src/test/java/kr/douid/brand/shared/exception/BusinessExceptionTest.java`
- [x] T011 [US2] `ErrorCodeTest` 작성 — `apps/api/src/test/java/kr/douid/brand/shared/exception/ErrorCodeTest.java`

**Checkpoint**: US2 완료 — 예외 구분 처리 검증 완료

---

## Phase 5: User Story 3 — Public / Admin API 경로 경계 (Priority: P3)

**Goal**: `/api/public/**`는 인증 없이 접근 가능하고, `/api/admin/**`는 인증 없이 401을 반환한다.

**Independent Test**: `curl /api/public/health` → 200. `curl /api/admin/test` → 401.

- [x] T012 [US3] `SecurityConfig` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/config/SecurityConfig.java`
- [x] T013 [US3] `HealthController` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/presentation/HealthController.java`
- [x] T014 [US3] `SecurityConfigTest` 작성 — `apps/api/src/test/java/kr/douid/brand/shared/config/SecurityConfigTest.java`

**Checkpoint**: US3 완료 — Public/Admin 경계 보호 검증 완료

---

## Phase 6: User Story 4 — 페이지네이션 응답 구조 (Priority: P3)

**Goal**: 목록 조회 API가 `content`, `page`, `size`, `totalElements`, `hasNext`를 일관된 구조로 반환한다.

**Independent Test**: `PageResponse.from(page)` 호출 시 올바른 필드 값 반환 확인.

- [x] T015 [US4] `PageResponse<T>` 구현 — `apps/api/src/main/java/kr/douid/brand/shared/pagination/PageResponse.java`
- [x] T016 [US4] `PageResponseTest` 작성 — `apps/api/src/test/java/kr/douid/brand/shared/pagination/PageResponseTest.java`

**Checkpoint**: US4 완료 — 페이지네이션 구조 재사용 가능

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: shared 패키지 경계 검증 및 전체 통합 확인

- [x] T017 [P] `shared` 패키지 도메인 의존성 검증 — 위반 없음 확인
- [x] T018 [P] `application.yml` 설정 — `apps/api/src/main/resources/application.yml` (open-in-view, jackson UTC)
- [x] T019 전체 테스트 실행 — `./gradlew test` 19개 통과 (DouidBrandApiApplicationTests는 DB 필요로 @Disabled)

---

## Dependencies & Execution Order

### Phase 의존 관계

- **Phase 1 (Setup)**: 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 후 — **모든 US를 블로킹**
- **Phase 3~6 (US1~US4)**: Phase 2 완료 후 시작 가능
  - US1(P1) → US2(P2) 순서 권장 (US2가 US1 GlobalExceptionHandler에 의존)
  - US3, US4는 US1 완료 후 병렬 진행 가능
- **Phase 7 (Polish)**: 모든 US 완료 후

### User Story 의존 관계

- **US1**: Phase 2 완료 후 시작 — 독립 구현 가능
- **US2**: US1 완료 후 시작 — GlobalExceptionHandler 존재 전제
- **US3**: US1 완료 후 시작 — ApiResponse, ErrorResponse 사용
- **US4**: Phase 2 완료 후 시작 — 독립 구현 가능 (US1과 병렬 가능)

### Parallel Opportunities

```bash
# Phase 2 병렬 실행 가능
T003 ErrorCode, T004 BusinessException, T005 BaseTimeEntity

# Phase 3 병렬 실행 가능
T006 ApiResponse, T007 ErrorResponse

# Phase 6~7 병렬 실행 가능
T015 PageResponse, T017 shared 패키지 경계 확인, T018 application.yml 설정
```

---

## Implementation Strategy

### MVP (US1만 구현)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료
3. Phase 3: US1 완료 (T006~T009)
4. **검증**: 성공/실패 응답 구조 동작 확인
5. 나머지 US는 순차 추가

### 전체 구현 순서

1. Setup → Foundational
2. US1 → US2 (예외 처리 기반 완성)
3. US3 + US4 병렬 진행 (경로 경계 + 페이지네이션)
4. Polish

---

## Notes

- `[P]` 태스크는 다른 파일을 수정하므로 병렬 실행 가능
- `[USN]` 레이블로 각 태스크가 어느 스토리에 속하는지 추적 가능
- 각 Phase Checkpoint에서 독립 검증 후 다음 Phase 진행
- `BaseTimeEntity`는 `shared/entity/`에 위치하지만 실제 도메인 엔티티(Work 등)가 상속할 때 비로소 DB 스키마에 반영됨
