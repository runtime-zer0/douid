# Feature Boundary Dependencies

## Context

`work`, `category`, `media`는 같은 Portfolio Management bounded context 안의 feature module이다. 세 feature는 독립적인 저장소 구현과 Aggregate 경계를 가지지만, 비즈니스상 관계가 있다.

- Work는 Category를 `categoryId`로 참조한다.
- Work는 Media를 `mediaId`로 참조한다.
- Category는 Work가 참조 중이면 삭제할 수 없다.
- Media는 Work가 참조 중이면 삭제할 수 없다.

따라서 feature 간 협력을 완전히 제거하는 것은 목표가 아니다. 목표는 협력이 필요한 지점을 명시하되, 상대 feature의 persistence 구현 세부사항을 알지 않게 하는 것이다.

## Boundary Model

이 문서의 결정은 `work`, `category`, `media`를 완전히 독립적인 bounded context로 분리하려는 설계가 아니다.

세 feature는 같은 Portfolio Management bounded context 안에서 서로 협력하는 feature module로 본다. 따라서 서로 아무것도 모르는 구조를 목표로 하지 않는다. 대신 다음 경계를 지킨다.

- feature 간 협력은 공개된 application contract로만 수행한다.
- 상대 feature의 infrastructure/JPA 구현은 알지 않는다.
- domain은 다른 feature의 application/infrastructure에 의존하지 않는다.
- persistence 구현 세부사항은 각 feature 내부에 숨긴다.

즉 분리 대상은 비즈니스 관계 자체가 아니라 구현 세부사항이다. 관계는 application port로 명시하고, 저장 구현은 feature 내부에 둔다.

## Problem

기존 구조에는 다음과 같은 의존이 생길 수 있었다.

```text
work.infrastructure
  -> category.infrastructure.persistence.CategoryJpaRepository

work.infrastructure
  -> media.infrastructure.persistence.MediaJpaRepository
```

이 구조는 Work가 필요한 능력인 "Category 존재 확인", "Media 사용 가능 여부 확인"을 얻기 위해 상대 feature의 JPA repository를 직접 참조한다.

문제는 다음과 같다.

- 한 feature의 저장 구현 변경이 다른 feature로 전파된다.
- feature 간 협력 지점이 application flow가 아니라 infrastructure adapter에 숨는다.
- 테스트가 JPA repository 세부사항에 가까워진다.
- feature 경계가 "공개 contract"가 아니라 "현재 구현체"를 기준으로 연결된다.

반대로 구현체만 `category.infrastructure`나 `media.infrastructure`로 옮기고 port를 `work.application.port`에 그대로 두면, 의존 방향만 바뀔 뿐 다른 feature의 port를 구현하는 결합이 남는다.

```text
category.infrastructure
  -> work.application.port.CategoryExistenceChecker
```

따라서 단순한 파일 위치 이동이 아니라, port의 소유권을 재정의해야 한다.

## Decision

다른 feature가 필요로 하는 조회 능력은 데이터를 소유한 feature가 application port로 공개한다.

```text
category.application.port.CategoryExistenceChecker
  <- category.infrastructure.port.JpaCategoryExistenceCheckerAdapter
       -> category.infrastructure.persistence.CategoryJpaRepository

media.application.port.MediaUsageValidator
  <- media.infrastructure.port.JpaMediaUsageValidatorAdapter
       -> media.infrastructure.persistence.MediaJpaRepository

work.application.command.WorkCommandService
  -> category.application.port.CategoryExistenceChecker
  -> media.application.port.MediaUsageValidator
```

Work 참조 여부 확인도 같은 기준을 따른다.

```text
work.application.port.WorkReferenceChecker
  <- work.infrastructure.port.JpaWorkReferenceCheckerAdapter
       -> work.infrastructure.persistence.WorkJpaRepository

category.application.command.CategoryCommandService
  -> work.application.port.WorkReferenceChecker

media.application.command.MediaCommandService
  -> work.application.port.WorkReferenceChecker
```

## Rules

- `work`, `category`, `media`는 같은 bounded context 안의 feature module로 다룬다.
- feature 간 협력은 application port 수준에서 허용한다.
- 다른 feature의 infrastructure 구현체나 JPA repository를 직접 참조하지 않는다.
- domain은 다른 feature의 application/infrastructure에 의존하지 않는다.
- JPA 연관관계는 같은 Aggregate 내부에서만 사용하고, 다른 Aggregate는 ID reference를 우선한다.
- DB FK는 application 검증을 대체하지 않고 참조 무결성의 최후 방어선으로 유지한다.

## Rationale

이 방식을 선택한 이유는 규칙 때문이 아니라 변경 영향과 협력 지점을 안정적으로 관리하기 위해서다.

Work가 Category에 대해 알아야 하는 것은 `CategoryJpaRepository`가 아니라 "이 categoryId가 존재하는가"라는 능력이다. Category가 Work에 대해 알아야 하는 것도 Work 테이블 구조가 아니라 "이 categoryId를 참조하는 Work가 있는가"라는 능력이다.

application port는 이 능력을 공개 contract로 표현한다. 구현체는 데이터를 소유한 feature의 infrastructure 안에 두므로, JPA repository나 QueryDSL, 캐시, 외부 API 등 저장 구현이 바뀌어도 호출 feature는 contract만 유지하면 영향을 받지 않는다.

이 구조는 다음 장점이 있다.

- feature 간 관계를 숨기지 않고 application flow에 명시한다.
- persistence 구현 세부사항이 feature 경계를 넘지 않는다.
- application service 테스트에서 상대 feature repository 대신 port mock으로 흐름을 검증할 수 있다.
- DB FK가 동시성이나 누락된 검증에 대한 최후 방어선 역할을 한다.

## Alternatives

### Infrastructure Adapter Direct Reference

```text
work.infrastructure
  -> category.infrastructure.persistence.CategoryJpaRepository
```

구현은 단순하지만, feature 간 경계가 persistence 구현체에 의해 연결된다. 저장 방식 변경의 영향이 커지고, application에서 어떤 협력이 필요한지 드러나지 않는다.

### Upper Orchestration Use Case

```text
portfolio.application.DeleteCategoryUseCase
  -> category application port
  -> work application port
```

feature 간 직접 의존을 더 줄일 수 있다. 여러 feature를 조합하는 유스케이스가 많거나 모듈 소유권이 강하게 분리될 때 적합하다. 현재 구조에서는 `work`, `category`, `media`가 같은 제품 도메인 안의 가까운 feature이므로 우선 application port 협력을 선택한다.

### DB FK Only

```text
category delete
  -> FK violation
  -> CategoryHasWorksException
```

참조 무결성은 강하게 보장하지만, 비즈니스 규칙이 application flow에 드러나지 않는다. 따라서 DB FK는 최후 방어선으로 유지하고, application service에서 먼저 명시적으로 검증한다.

### Event Listener

삭제 차단, 존재 검증, 상태 전이 같은 핵심 흐름은 event listener에 위임하지 않는다. 이벤트는 저장 후 WebSocket 발행, 알림, 로그, 캐시 무효화처럼 커밋 이후의 부가 작업에 사용한다.

## Current Direction

문서와 코드의 목표 구조는 다음과 같다.

```text
work.application
  -> category.application.port.CategoryExistenceChecker
  -> media.application.port.MediaUsageValidator

category.application
  -> work.application.port.WorkReferenceChecker

media.application
  -> work.application.port.WorkReferenceChecker
```

금지되는 구조는 다음과 같다.

```text
work.infrastructure
  -> category.infrastructure

work.infrastructure
  -> media.infrastructure

category.infrastructure
  -> work.infrastructure

media.infrastructure
  -> work.infrastructure
```
