# Research: Shared Foundation

**Date**: 2026-06-15

## 1. 공통 API 응답 구조

**Decision**: `ApiResponse<T>` 단일 래퍼 클래스 사용. 성공/실패를 `success` 필드로 구분.

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": { "code": "INVALID_INPUT", "message": "...", "fields": [...] } }
```

**Rationale**: 클라이언트가 `success` 하나만 보고 분기할 수 있어 처리 로직이 단순해진다. HTTP 상태 코드와 병행 사용.

**Alternatives considered**:
- `code`/`message` 최상위 flat 구조 → 성공/실패 구분이 명시적이지 않아 기각
- Spring 기본 `ProblemDetail` (RFC 9457) → 외부 표준이지만 `success` 필드가 없어 클라이언트 일관성 저하, 기각

---

## 2. 예외 처리 전략

**Decision**: `BusinessException` (검증된 비즈니스 예외) + `@RestControllerAdvice` GlobalExceptionHandler 조합.

- `BusinessException`: `ErrorCode` enum을 받아 생성. HTTP 상태도 ErrorCode에 포함.
- `MethodArgumentNotValidException`: Bean Validation 실패 → 필드별 오류 목록 반환.
- 나머지 `Exception`: 500 Internal Server Error, 상세 정보 미노출.

**Rationale**: 비즈니스 예외를 명시적으로 분리하면 이후 도메인별 ErrorCode 추가 시 GlobalExceptionHandler를 수정하지 않아도 된다.

**Alternatives considered**:
- 각 Controller마다 `@ExceptionHandler` → 중복 발생, 기각
- `ResponseStatusException` 직접 사용 → ErrorCode 중앙 관리 불가, 기각

---

## 3. 에러 코드 관리

**Decision**: `ErrorCode` enum에 HTTP 상태, 코드 문자열, 기본 메시지를 함께 정의.

```java
INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다.")
INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.")
UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.")
```

**Rationale**: 코드-상태-메시지를 한 곳에서 관리해 이후 도메인별 ErrorCode 추가 시 패턴 일관성 유지.

---

## 4. 공통 시간 관리

**Decision**: `BaseTimeEntity` abstract 클래스. `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate`.

**Rationale**: Spring Data JPA Auditing이 이미 의존성에 포함되어 있어 별도 라이브러리 추가 없이 구현 가능.

**Alternatives considered**:
- `@PrePersist` / `@PreUpdate` 직접 구현 → Auditing 활성화만으로 해결되는 것을 수동 처리, 기각

---

## 5. 페이지네이션 응답

**Decision**: `PageResponse<T>` 레코드(또는 불변 클래스). Spring Data `Page<T>`를 받아 변환하는 정적 팩토리 메서드 제공.

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 100, "hasNext": true }
```

**Rationale**: Spring Data `Page` 직접 반환 시 직렬화 결과가 장황하고 불안정함. 래퍼로 감싸 필요한 필드만 노출.

---

## 6. Public / Admin 경로 경계

**Decision**: `SecurityConfig`에서 `/api/public/**` permitAll, `/api/admin/**` authenticated 설정. 실제 인증 구현(JWT 등)은 이후 단계.

**Rationale**: 경로 기반 경계를 지금 정의해두면 이후 Admin 기능이 올바른 위치에 배치되고 보안 설정 누락을 방지.

**Alternatives considered**:
- 인증 구현과 함께 나중에 추가 → Admin 경로가 일시적으로 열린 채 구현될 위험, 기각
