# Implementation Plan: Category Management

**Branch**: `feature/4-category-management` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-category-management/spec.md`

## Summary

작업물 분류를 위한 카테고리 도메인과 관리자/공개 API를 구현한다.
`category` 패키지를 `domain / application / presentation / infrastructure` 4레이어로 구성하고,
슬러그 중복 검증, 공개 여부 필터, 노출 순서 정렬을 포함한 CRUD를 제공한다.
Work 연결 삭제 제한은 `CategoryDeletionPolicy` 인터페이스로 확장 지점만 준비한다.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Spring Boot 4, Spring Web MVC, Spring Security, Spring Data JPA, Spring Validation, Lombok
**Storage**: PostgreSQL (Testcontainers로 테스트)
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers
**Target Platform**: Linux server (REST API)
**Project Type**: web-service
**Performance Goals**: 일반 웹 서비스 수준 (카테고리 목록은 전체 반환, 페이지네이션 없음)
**Constraints**: category 패키지가 work, media, chat, ai 패키지를 import하지 않아야 함
**Scale/Scope**: 브랜드 포트폴리오 단일 서버 — 카테고리 수십 건 수준

## Constitution Check

CLAUDE.md 기준 주요 규칙 점검:

| 규칙 | 상태 | 비고 |
|------|------|------|
| Feature-first 패키지: `category/{domain,application,presentation,infrastructure}` | ✅ | root-level 패키지 신규 생성 없음 |
| `shared`에 category 비즈니스 규칙 없음 | ✅ | ErrorCode 추가만 허용 (공통 기술 요소) |
| domain이 presentation/infrastructure에 의존 안 함 | ✅ | `Category`, `CategoryRepository`, `CategoryDeletionPolicy`는 순수 domain |
| application이 HTTP DTO에 직접 의존 안 함 | ✅ | Controller → Service 흐름, DTO는 presentation 레이어에만 존재 |
| controller가 repository를 직접 호출하지 않음 | ✅ | Controller → Service → Repository |
| JPA 기본 생성자 `protected` | ✅ | Category 엔티티에 적용 |
| public setter 금지, domain method로 상태 변경 | ✅ | `Category.update()` domain method 사용 |
| static factory method 사용 | ✅ | `Category.create(...)` |
| Admin API 임시 `.permitAll()` (Auth 단계 전) | ✅ | SecurityConfig 변경 예정 |

**Gate 결과**: 모두 통과. 구현 진행 가능.

## Project Structure

### Documentation (this feature)

```text
specs/002-category-management/
├── plan.md              # This file
├── data-model.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── category-api.md
└── tasks.md             # /speckit.tasks output
```

### Source Code

```text
apps/api/src/main/java/kr/douid/brand/
└── category/
    ├── domain/
    │   ├── Category.java                      # Aggregate Root (Entity)
    │   ├── CategoryRepository.java            # Port (interface)
    │   └── CategoryDeletionPolicy.java        # Port (interface, 확장 지점)
    ├── application/
    │   ├── CategoryCommandService.java        # 생성/수정/삭제
    │   └── CategoryQueryService.java          # 목록 조회
    ├── presentation/
    │   ├── AdminCategoryController.java       # POST/PUT/DELETE/GET /api/admin/categories
    │   ├── PublicCategoryController.java      # GET /api/public/categories
    │   └── dto/
    │       ├── CreateCategoryRequest.java
    │       ├── UpdateCategoryRequest.java
    │       └── CategoryResponse.java
    └── infrastructure/
        ├── JpaCategoryRepository.java         # Spring Data JPA 구현체
        └── DefaultCategoryDeletionPolicy.java # 현재 단계: 항상 허용

apps/api/src/main/java/kr/douid/brand/shared/
├── exception/
│   └── ErrorCode.java    # CATEGORY_NOT_FOUND, CATEGORY_SLUG_DUPLICATE, CATEGORY_HAS_WORKS 추가
└── config/
    └── SecurityConfig.java    # /api/admin/** 임시 permitAll() 변경

apps/api/src/test/java/kr/douid/brand/
└── category/
    ├── domain/
    │   └── CategoryTest.java
    ├── application/
    │   ├── CategoryCommandServiceTest.java
    │   └── CategoryQueryServiceTest.java
    ├── presentation/
    │   ├── AdminCategoryControllerTest.java
    │   └── PublicCategoryControllerTest.java
    └── infrastructure/
        └── JpaCategoryRepositoryTest.java
```

## Complexity Tracking

> 이번 단계에서 Constitution 위반 없음. 기록 불필요.
