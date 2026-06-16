# API Contract: 공통 응답 형식

**Date**: 2026-06-15

## 성공 응답

```json
{
  "success": true,
  "data": { }
}
```

- `data`는 단일 객체 또는 `PageResponse` 구조체
- 응답 본문이 없을 경우 `data: null`

## 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다.",
    "fields": [
      { "field": "title", "message": "제목은 필수입니다." }
    ]
  }
}
```

- `fields`는 Bean Validation 실패 시에만 포함
- 시스템 오류(500)의 경우 `fields` 미포함, `message`는 일반 메시지

## 페이지네이션 목록 응답

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "hasNext": true
  }
}
```

- `page`: 0-based 페이지 번호
- `hasNext`: `(page + 1) * size < totalElements`

## HTTP 상태 코드

| 상황 | 상태 코드 |
|------|-----------|
| 정상 조회/처리 | 200 OK |
| 생성 성공 | 201 Created |
| 응답 본문 없음 | 204 No Content |
| 요청 값 오류 | 400 Bad Request |
| 인증 필요 | 401 Unauthorized |
| 권한 없음 | 403 Forbidden |
| 리소스 없음 | 404 Not Found |
| 서버 오류 | 500 Internal Server Error |

## 경로 경계

| 경로 패턴 | 인증 필요 |
|-----------|-----------|
| `/api/public/**` | 불필요 |
| `/api/admin/**` | 필요 (401 반환) |
