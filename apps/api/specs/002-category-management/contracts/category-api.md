# API Contract: Category Management

**Date**: 2026-06-16

## 관리자 API

### POST /api/admin/categories — 카테고리 생성

**Request**
```json
{
  "name": "브랜딩",
  "slug": "branding",
  "displayOrder": 1,
  "visible": true
}
```

- `name`: 필수, 공백만 있으면 실패
- `slug`: 필수, 공백만 있으면 실패
- `displayOrder`: 선택, 기본값 0
- `visible`: 선택, 기본값 true

**Response 201 Created**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "name": "브랜딩",
    "slug": "branding",
    "displayOrder": 1,
    "visible": true,
    "createdAt": "2026-06-16T00:00:00",
    "updatedAt": "2026-06-16T00:00:00"
  }
}
```

**Response 409 Conflict** (슬러그 중복)
```json
{
  "status": "FAILURE",
  "data": {
    "code": "CATEGORY_SLUG_DUPLICATE",
    "message": "이미 사용 중인 슬러그입니다."
  }
}
```

**Response 400 Bad Request** (검증 실패)
```json
{
  "status": "FAILURE",
  "data": {
    "code": "INVALID_INPUT",
    "message": "요청 값이 올바르지 않습니다.",
    "fields": [
      { "field": "name", "message": "공백일 수 없습니다" }
    ]
  }
}
```

---

### PUT /api/admin/categories/{id} — 카테고리 수정

**Request**
```json
{
  "name": "브랜딩 v2",
  "slug": "branding-v2",
  "displayOrder": 2,
  "visible": false
}
```

**Response 200 OK** — 수정된 `CategoryResponse` 동일 구조

**Response 404 Not Found**
```json
{
  "status": "FAILURE",
  "data": {
    "code": "CATEGORY_NOT_FOUND",
    "message": "카테고리를 찾을 수 없습니다."
  }
}
```

**Response 409 Conflict** — 슬러그 중복 (자기 자신 제외)

---

### DELETE /api/admin/categories/{id} — 카테고리 삭제

**Response 204 No Content** (성공)

**Response 404 Not Found** — 존재하지 않는 카테고리

---

### GET /api/admin/categories — 관리자 카테고리 목록

**Response 200 OK**
```json
{
  "status": "SUCCESS",
  "data": [
    {
      "id": 1,
      "name": "브랜딩",
      "slug": "branding",
      "displayOrder": 1,
      "visible": true,
      "createdAt": "2026-06-16T00:00:00",
      "updatedAt": "2026-06-16T00:00:00"
    }
  ]
}
```

- 공개/비공개 카테고리 모두 포함
- `displayOrder` ASC, `createdAt` ASC 정렬
- 페이지네이션 없음 (전체 반환)

---

## 공개 API

### GET /api/public/categories — 공개 카테고리 목록

**Response 200 OK** — 관리자 목록과 동일 구조

- `visible = true`인 카테고리만 포함
- `displayOrder` ASC, `createdAt` ASC 정렬

## HTTP 상태 코드 요약

| 상황 | 상태 코드 |
|------|-----------|
| 생성 성공 | 201 Created |
| 조회/수정 성공 | 200 OK |
| 삭제 성공 | 204 No Content |
| 검증 실패 | 400 Bad Request |
| 리소스 없음 | 404 Not Found |
| 슬러그 중복 | 409 Conflict |
