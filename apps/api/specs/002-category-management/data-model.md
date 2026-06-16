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

### CategoryDeletionPolicy (Port interface)

삭제 가능 여부를 판단하는 정책 확장 지점. domain 레이어에 위치.

```
interface CategoryDeletionPolicy {
    void validate(Category category);  // 위반 시 BusinessException 던짐
}
```

- `DefaultCategoryDeletionPolicy`: 현재 단계에서는 아무 검사 없이 통과
- Work 단계에서 `WorkLinkedCategoryDeletionPolicy`로 교체 예정

## 관계

```
Category (domain)
  ↑ extends
BaseTimeEntity (shared)

CategoryRepository (domain port)
  ↓ implements
JpaCategoryRepository (infrastructure)

CategoryDeletionPolicy (domain port)
  ↓ implements
DefaultCategoryDeletionPolicy (infrastructure)

CategoryCommandService / CategoryQueryService (application)
  → CategoryRepository (port)
  → CategoryDeletionPolicy (port)

AdminCategoryController / PublicCategoryController (presentation)
  → CategoryCommandService / CategoryQueryService (application)
```

## 정렬 기준

- 1순위: `displayOrder` ASC
- 2순위: `createdAt` ASC (displayOrder 동일 시)
