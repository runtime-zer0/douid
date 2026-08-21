# API Contracts — Admin Authentication

Base path: `/api/auth`

모든 응답은 `ResponseEntity<ApiResponse<T>>` 래핑을 따른다. 로그인은 인증 없이 접근 가능하고, 현재 관리자 조회·로그아웃은 인증된 Session이 필요하다.

---

## POST /api/auth/login

관리자 로그인. 인증 불필요(permitAll).

### Request Body

```json
{
  "email": "string (required, @Email)",
  "password": "string (required, not blank)"
}
```

### Responses

| Status | 설명 |
|---|---|
| 200 | 로그인 성공 — Session Cookie(`Set-Cookie: JSESSIONID=...`) 발급 + 관리자 최소 정보 반환 |
| 400 | 요청 본문이 JSON이 아니거나 필수 필드 누락/형식 오류 (`MethodArgumentNotValidException`) |
| 401 | 계정 미존재 / 비밀번호 불일치 / 비활성 계정 — 세 경우 모두 동일한 응답 |

### 성공 응답 예시

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "admin@example.com",
    "role": "ADMIN"
  }
}
```

### 실패 응답 예시 (401 — 계정 미존재/비밀번호 불일치/비활성 계정 공통)

```json
{
  "success": false,
  "error": {
    "code": "AUTHENTICATION_FAILED",
    "message": "이메일 또는 비밀번호가 올바르지 않습니다."
  }
}
```

**계약 노트**: 위 세 실패 케이스는 코드·메시지·HTTP status가 완전히 동일해야 한다(spec.md FR-005). 응답만으로 계정 존재 여부를 구분할 수 없다.

---

## GET /api/auth/me

현재 인증된 관리자 정보 조회. 인증 필요.

### Responses

| Status | 설명 |
|---|---|
| 200 | 인증된 관리자의 최소 정보 반환 |
| 401 | Session 없음 또는 유효하지 않음 |

### 성공 응답 예시

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "admin@example.com",
    "role": "ADMIN"
  }
}
```

password 및 내부 보안 정보(passwordHash 등)는 응답에 포함하지 않는다(spec.md FR-007).

---

## POST /api/auth/logout

관리자 로그아웃. 인증 필요.

### Responses

| Status | 설명 |
|---|---|
| 200 | 로그아웃 성공 — 현재 Session 무효화 |
| 401 | 이미 인증되지 않은 상태 |

### 성공 응답 예시

```json
{
  "success": true,
  "data": null
}
```

로그아웃 성공 후 클라이언트가 보유하던 Session Cookie로 보호된 Admin API를 재호출하면 401이 반환되어야 한다(spec.md FR-008).

---

## GET /api/auth/csrf-token

CSRF 토큰 발급/조회용 endpoint. 인증 불필요(permitAll) — Frontend가 로그인 이전에도 토큰을 얻을 수 있어야 상태 변경 요청(로그인 자체는 GET이라 CSRF 대상 아니지만, 로그인 이후 첫 상태 변경 요청부터는 토큰이 필요) 흐름이 끊기지 않는다.

### Responses

| Status | 설명 |
|---|---|
| 200 | `XSRF-TOKEN` 쿠키가 없으면 발급하고, 현재 토큰 값을 함께 응답 본문에도 포함 |

### 성공 응답 예시

```json
{
  "success": true,
  "data": {
    "headerName": "X-XSRF-TOKEN",
    "token": "..."
  }
}
```

**계약 노트**: 이 endpoint는 `CsrfToken`을 강제로 리졸브시켜(`request.getAttribute(CsrfToken.class.getName())`) 지연 로딩된 토큰을 쿠키로 내려보내는 트리거 역할만 한다. 별도 비즈니스 로직은 없다.

---

## 기존 Admin API에 적용되는 공통 계약 (신규 endpoint 아님)

Category/Media/Work의 기존 Admin API는 엔드포인트·요청/응답 스펙 자체는 변경하지 않는다. 다음 공통 규칙만 추가된다.

| 상황 | Status | 응답 |
|---|---|---|
| Session 없음/무효 | 401 | `{ "success": false, "error": { "code": "UNAUTHORIZED", ... } }` |
| 인증되었으나 ADMIN 권한 없음 | 403 | `{ "success": false, "error": { "code": "FORBIDDEN", ... } }` |

두 경우 모두 로그인 페이지로 Redirect하지 않고 JSON으로만 응답한다(spec.md FR-011).
