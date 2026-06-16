# Quickstart: Shared Foundation

**Date**: 2026-06-15

## 구현 대상

`kr.douid.brand.shared` 패키지에 다음을 구현한다.

## 1. 성공/실패 응답 사용

```java
// 성공
return ResponseEntity.ok(ApiResponse.ok(result));

// 생성
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));

// 실패 (GlobalExceptionHandler가 자동 처리)
throw new BusinessException(ErrorCode.NOT_FOUND);
```

## 2. 새 에러 코드 추가 (이후 도메인 단계)

`ErrorCode` enum에 추가:

```java
WORK_NOT_FOUND(HttpStatus.NOT_FOUND, "WORK_NOT_FOUND", "작업물을 찾을 수 없습니다."),
```

## 3. 도메인 엔티티에서 BaseTimeEntity 상속

```java
@Entity
public class Work extends BaseTimeEntity {
    // createdAt, updatedAt 자동 관리
}
```

## 4. 목록 조회 응답

```java
Page<WorkSummary> page = workQueryService.findAll(pageable);
return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
```

## 5. 서버 실행 및 확인

```bash
cd apps/api
./gradlew bootRun

# Public 경로 (인증 불필요)
curl http://localhost:8080/api/public/health

# Admin 경로 (인증 필요 → 401 반환)
curl http://localhost:8080/api/admin/test
```

## 테스트 실행

```bash
./gradlew test
```
