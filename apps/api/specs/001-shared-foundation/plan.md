# Implementation Plan: Shared Foundation

**Branch**: `feature/2-shared-foundation` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-shared-foundation/spec.md`

## Summary

Public, Admin, Integrated Chat System 영역이 공통으로 의존할 서버 기반을 구축한다.
공통 API 응답 형식, 예외 처리, 에러 코드, 시간 자동 관리, 페이지네이션 응답, Public/Admin 경로 보안 경계를 `shared` 패키지 안에 구현한다.
비즈니스 도메인(work, category, media, chat, ai)에 대한 의존 없이 순수 기술 기반만 포함한다.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Spring Boot 4, Spring Web MVC, Spring Security, Spring Data JPA, Spring Validation, Lombok
**Storage**: PostgreSQL (Testcontainers로 테스트)
**Testing**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers
**Target Platform**: Linux server (REST API)
**Project Type**: web-service
**Performance Goals**: 일반 웹 서비스 수준 (별도 SLA 없음)
**Constraints**: shared 패키지가 비즈니스 도메인 패키지를 import하지 않아야 함
**Scale/Scope**: 브랜드 포트폴리오 단일 서버

## Constitution Check

CLAUDE.md 기준 주요 규칙 점검:

| 규칙 | 상태 | 비고 |
|------|------|------|
| Feature-first 패키지: `shared/{config,security,exception,response,pagination}` | ✅ | 공통 기술 요소만 포함 |
| `shared`에 feature-specific 비즈니스 규칙 없음 | ✅ | 이번 단계는 순수 기술 기반 |
| domain이 presentation/infrastructure에 의존 안 함 | ✅ | shared는 domain 레이어 없음 |
| application이 HTTP DTO에 직접 의존 안 함 | ✅ | 이번 단계는 application 레이어 미포함 |
| root-level 패키지 신규 생성 금지 | ✅ | shared 하위에만 추가 |
| JPA 기본 생성자 `protected` | ✅ | BaseTimeEntity에 적용 |
| public setter 금지 | ✅ | 응답 객체는 불변 |

**Gate 결과**: 모두 통과. Phase 1 진행 가능.

## Project Structure

### Documentation (this feature)

```text
specs/001-shared-foundation/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── api-response.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code

```text
apps/api/src/main/java/kr/douid/brand/
└── shared/
    ├── config/
    │   └── SecurityConfig.java          # Public/Admin 경로 경계 설정
    ├── exception/
    │   ├── BusinessException.java       # 비즈니스 예외 기반 클래스
    │   ├── ErrorCode.java               # 공통 에러 코드 열거형
    │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
    ├── response/
    │   ├── ApiResponse.java             # 성공 응답 래퍼
    │   └── ErrorResponse.java           # 실패 응답 구조
    └── pagination/
        └── PageResponse.java            # 페이지네이션 응답 구조

apps/api/src/main/java/kr/douid/brand/
└── work/ (또는 임의 도메인)
    └── domain/
        └── BaseTimeEntity.java          # createdAt/updatedAt 공통 엔티티

apps/api/src/test/java/kr/douid/brand/
└── shared/
    ├── exception/
    │   └── GlobalExceptionHandlerTest.java
    └── response/
        └── ApiResponseTest.java
```

## Complexity Tracking

> 이번 단계에서 Constitution 위반 없음. 기록 불필요.
