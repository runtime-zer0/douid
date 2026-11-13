# Phase 07·08 Legacy Chat 제거

## 결정과 범위

Phase 07·08의 서버 측 비회원 신원, 대화 저장, 이메일 인증 및 Magic Link 복원 구현을 제거한다.
후속 AI 챗봇은 브라우저의 Zustand + sessionStorage로 대화를 관리하며 대화 원문을 DB에 저장하지 않는다.
이번 변경은 새 챗봇이나 AI API를 구현하지 않는다.

기존 `specs/007-anonymous-client-identity`, `specs/008-verified-email-recovery`는 폐기된 구현의 역사 자료다.
해당 문서의 ClientIdentity 재사용, Conversation 저장 및 복원 요구사항은 후속 챗봇의 요구사항이 아니다.
Phase 01~06 명세와 기존 CLAUDE.md 지침 파일 구조는 보존한다.

## 변경 전 확인

- 최초 작업 트리 변경은 `PostgresIntegrationTest.java` 한 파일이며 원문을 보존
- `cd661ce`: Phase 06의 Admin Session 로그인/로그아웃 필터 구성
- `f6c1173`: Phase 07 ClientIdentity/ClientCredential, Conversation/Message 및 쿠키 인증 추가
- `bb3b9ea`: Phase 08 이메일 인증, Magic Link, Redis rate limit 추가
- `36b7c49`: CMS와 Legacy Chat을 함께 포함하는 Flyway V1 baseline 도입
- `38dbeb9`, `32ea2d7`: 이메일 유일성 및 상담 시작 동시성 처리 변경
- 실제 import와 호출부 확인 결과, CMS 운영 코드는 client/chat에 의존하지 않음
- CMS/Auth Controller 테스트 7곳의 ClientAuthenticationService mock만 제거 필요
- 기존 Next.js 소스에서 Legacy API 및 client_token 호출 참조 없음

## 제거

- `client`, `chat`의 Java 운영 코드와 전용 테스트: Controller, application service/port, domain, Repository 및 adapter
- ClientCredentialFilter, ClientIdentityContext, ClientTokenCookieProvider, ClientCookieProperties
- RedisConfig 및 이메일 인증·복원·쿠키 설정
- 상담 전용 CursorPageResponse 및 Phase 08 전용 TOO_MANY_REQUESTS 매핑
- Phase 08에서 도입한 Mail starter
- `/api/client/**`의 permitAll: 기존 denyAll 정책으로 차단

## 보존

- Category, Work, Media의 CMS 운영 코드, 공개 조회 및 Admin API
- Admin 초기화, BCrypt, Session SecurityContext 저장/복원, 로그인/로그아웃 필터
- CSRF, CORS, ADMIN 권한 검사, 공통 오류 응답
- Phase 07 이전부터 있던 Redis·WebSocket starter 및 대응 테스트 starter
- Flyway V1 원문과 기존 DB의 모든 테이블 및 데이터
- `PostgresIntegrationTest.java`의 미커밋 변경과 기존 메일 테스트 프로퍼티까지 원문 그대로 유지
- Next.js 전체, Phase 01~06 명세 및 프로젝트 지침 파일

## DB 안전 경계

2026-11-13 로컬 Docker `postgres`의 `douid_postgres_db`에서 READ ONLY 트랜잭션으로
테이블별 행 수만 확인했다. admins, categories, media, works, work_media와 아래 7개 Legacy 테이블은
모두 0행이었다. 개인정보, 토큰, 대화 원문은 조회하지 않았다. 운영 DB는 확인하지 않았다.

기존 DB에 migration을 실행하지 않았다. V2 cleanup migration도 추가하지 않는다.
엔티티를 제거해도 Hibernate `ddl-auto=validate`는 남아 있는 비매핑 테이블을 삭제하지 않으므로,
코드 제거와 파괴적인 스키마 변경을 분리할 수 있다. 새 DB에서도 V1은 Legacy 빈 테이블을 생성한다.

향후 스키마 정리가 필요하면 별도 변경으로 아래 테이블을 검토한다.

| 삭제 검토 순서 | 테이블 |
| --- | --- |
| 1 | message |
| 2 | conversation |
| 3 | client_recovery_token |
| 4 | email_verification_challenge |
| 5 | client_email |
| 6 | client_credential |
| 7 | client_identity |

실제 대상 DB의 데이터를 다시 읽기 전용으로 확인하고, 데이터가 있으면 실행을 중단하고 보고한다.
백업 및 데이터 처리 결정 전에는 DROP/TRUNCATE/DELETE를 실행하지 않는다.
V1 수정, Flyway clean, CASCADE를 사용하지 않는다. CMS 테이블은 삭제 대상이 아니다.

## 검증

- 제거 전 보안 회귀 테스트: 폐기 대상 경로 7개에서 예상대로 실패, 기존 보안 검증 통과
- 기존 작업 디렉토리에서 `./gradlew clean build` 성공: 테스트 168개 통과, 실패·오류·건너뜀 0개
  - 이 실행에는 사용자의 미커밋 `PostgresIntegrationTest.java` 컨텍스트 폐기 수정이 포함됨
  - 해당 수정은 제거 커밋에서 제외하고 현재 디렉토리에 그대로 보존
- 수정이 제외된 별도 worktree의 기본 전체 테스트에서 컨테이너 재시작 후 DB 연결 실패 관찰
  - 클래스 간 Spring 컨텍스트 재사용과 클래스별 Testcontainer 재시작이 충돌하는 기존 테스트 구성
  - 검증용 외부 Gradle init script의 `forkEvery = 1`로 클래스별 JVM을 분리해 전체 테스트 실행
  - init script는 커밋에 포함하지 않으며 테스트를 제외하거나 skip하지 않음
- Admin 3개, Auth 9개, Category 29개, Media 32개, Work 56개, Shared 38개, 기동 1개
- SecurityConfig는 Phase 06 `cd661ce`의 파일과 바이트 단위로 동일
- clean 후 Legacy QueryDSL 생성 코드와 실행 JAR의 client/chat 클래스 및 Mail 라이브러리 없음
- 기존 Admin/CMS 테스트의 동작 검증 유지, Legacy 전용 mock만 삭제
- Admin 권한과 기존 client_token 쿠키를 함께 보내도 폐기 대상 경로의 GET/POST는 403
- 기존 client_token 쿠키만으로 Admin API에 접근하면 401
- 통합 테스트는 기존 DB 대신 별도 PostgreSQL Testcontainer와 Flyway V1 사용

기존 브라우저의 client_token은 더 이상 읽거나 발급하지 않는다. 쿠키 강제 삭제를 위한 별도 API는 추가하지 않는다.
제거된 API의 호출자는 더 이상 서버 상담 시작·메시지 저장·이메일 복원을 사용할 수 없다.
코드 롤백 시 보존된 DB를 그대로 사용할 수 있다. 원격 push 및 배포는 수행하지 않는다.
현재 디렉토리와 별도인 worktree에서 제거 변경만 커밋하며, 사용자의 기존 미커밋 변경은 포함하지 않는다.
