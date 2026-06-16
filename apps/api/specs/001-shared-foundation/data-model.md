# Data Model: Shared Foundation

**Date**: 2026-06-15

## 엔티티 / 값 객체

### BaseTimeEntity (abstract)

도메인 엔티티가 상속할 공통 시간 추상 클래스.

| 필드 | 타입 | 설명 |
|------|------|------|
| createdAt | LocalDateTime | 생성 시각 (자동, UTC) |
| updatedAt | LocalDateTime | 수정 시각 (자동, UTC) |

- JPA Auditing으로 자동 관리
- 직접 수정 불가 (setter 없음)
- 하위 엔티티가 `@Entity`를 선언

---

### ApiResponse\<T\> (응답 래퍼)

모든 성공 API 응답에 사용하는 불변 래퍼.

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 항상 `true` |
| data | T | 응답 데이터 (null 가능) |

정적 팩토리: `ApiResponse.ok(data)`, `ApiResponse.noContent()`

---

### ErrorResponse (실패 응답)

모든 실패 API 응답에 사용하는 불변 구조체.

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 항상 `false` |
| error.code | String | 에러 코드 문자열 |
| error.message | String | 사람이 읽을 수 있는 메시지 |
| error.fields | List\<FieldError\> | 필드 검증 오류 목록 (선택) |

FieldError: `{ field: String, message: String }`

---

### ErrorCode (열거형)

| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| INVALID_INPUT | 400 | 요청 값이 올바르지 않습니다. |
| UNAUTHORIZED | 401 | 인증이 필요합니다. |
| FORBIDDEN | 403 | 접근 권한이 없습니다. |
| NOT_FOUND | 404 | 요청한 리소스를 찾을 수 없습니다. |
| INTERNAL_SERVER_ERROR | 500 | 서버 오류가 발생했습니다. |

이후 도메인별 ErrorCode는 각 feature 패키지에서 추가.

---

### PageResponse\<T\> (페이지네이션 응답)

목록 조회 API에 사용하는 불변 래퍼.

| 필드 | 타입 | 설명 |
|------|------|------|
| content | List\<T\> | 현재 페이지 데이터 |
| page | int | 현재 페이지 번호 (0-based) |
| size | int | 페이지 크기 |
| totalElements | long | 전체 데이터 수 |
| hasNext | boolean | 다음 페이지 존재 여부 |

정적 팩토리: `PageResponse.from(Page<T> page)`

## 관계

```
[Client] → ApiResponse<T> or ErrorResponse
[Domain Entity] extends BaseTimeEntity
[List API] → ApiResponse<PageResponse<T>>
```
