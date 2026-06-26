# Data Model: Category Management

**Date**: 2026-06-16

## 엔티티 / 값 객체

### Category (Aggregate Root)

작업물 분류 단위. `BaseTimeEntity`를 상속한다.

| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK, auto-increment |
| name | String | not null, max 100 |
| slug | String | not null, unique, max 100 |
| displayOrder | int | not null, default 0 |
| visible | boolean | not null, default true |
| createdAt | LocalDateTime | BaseTimeEntity (자동, UTC) |
| updatedAt | LocalDateTime | BaseTimeEntity (자동, UTC) |

- JPA 기본 생성자: `protected`
- 생성: `Category.create(name, slug, displayOrder, visible)` static factory
- 상태 변경: `category.update(name, slug, displayOrder, visible)` domain method
- public setter 없음

---

### CategoryResponse (읽기 DTO)

API 응답에 사용하는 불변 구조체. presentation 레이어에만 존재.

| 필드 | 타입 |
|------|------|
| id | Long |
| name | String |
| slug | String |
| displayOrder | int |
| visible | boolean |
| createdAt | LocalDateTime |
| updatedAt | LocalDateTime |

정적 팩토리: `CategoryResponse.from(Category)`

---

### ErrorCode 추가 항목

| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| CATEGORY_NOT_FOUND | 404 | 카테고리를 찾을 수 없습니다. |
| CATEGORY_SLUG_DUPLICATE | 409 | 이미 사용 중인 슬러그입니다. |
| CATEGORY_HAS_WORKS | 409 | 작업물이 연결된 카테고리는 삭제할 수 없습니다. |

---

### WorkReferenceChecker (External application port)

카테고리 삭제 가능 여부 중 Work 참조 여부를 확인하는 외부 application port. category application delete flow에서 호출하며, category는 work infrastructure/domain 구현체에 직접 의존하지 않는다.

## 관계

```
Category (domain)
  ↑ extends
BaseTimeEntity (shared)

CategoryRepository (domain port)
  ↓ implements
JpaCategoryRepository (infrastructure)

CategoryCommandService / CategoryQueryService (application)
  → CategoryRepository (port)
  → WorkReferenceChecker (work application port)

AdminCategoryController / PublicCategoryController (presentation)
  → CategoryCommandService / CategoryQueryService (application)
```

## 정렬 기준

- 1순위: `displayOrder` ASC
- 2순위: `createdAt` ASC (displayOrder 동일 시)
