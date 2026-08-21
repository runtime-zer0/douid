# Quickstart: Admin Authentication

**Date**: 2026-08-21

## 사전 준비

로컬 개발 환경에서 초기 관리자 계정을 생성하려면 환경 변수를 지정한다.

```bash
export ADMIN_INIT_EMAIL="admin@douid.kr"
export ADMIN_INIT_PASSWORD="change-me-locally"
```

애플리케이션 최초 기동 시 `admins` 테이블이 비어 있으면 위 값으로 계정 하나가 생성된다. 이미 계정이 존재하면 아무 동작도 하지 않는다.

## 1. CSRF 토큰 발급 → 로그인 → Session Cookie 획득

로그인도 상태 변경 요청(POST)이라 CSRF 보호 대상이다. 로그인 전에 먼저 토큰을 발급받아야 한다.

응답 body의 `data.token`은 요청마다 랜덤하게 마스킹된 값이며, `XSRF-TOKEN` 쿠키의 raw 값과는 다르다. 헤더에는 반드시 응답 body의 `data.token`(마스킹된 값)을 그대로 실어야 하며, 쿠키 값을 직접 읽어 쓰면 안 된다.

```bash
# 토큰 발급 (Set-Cookie: XSRF-TOKEN=... 포함)
curl -i http://localhost:8080/api/auth/csrf-token -c cookies.txt

# 응답 body의 data.token 값을 헤더로 전달
XSRF_TOKEN=$(curl -s http://localhost:8080/api/auth/csrf-token -b cookies.txt -c cookies.txt | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")

curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $XSRF_TOKEN" \
  -b cookies.txt -c cookies.txt \
  -d '{"email":"admin@douid.kr","password":"change-me-locally"}'
```

`cookies.txt`에 `JSESSIONID`가 저장된다.

## 2. Session Cookie로 현재 관리자 조회

```bash
curl -i http://localhost:8080/api/auth/me -b cookies.txt
```

## 3. CSRF 토큰 재발급 후 상태 변경 요청

Session이 있어도 CSRF 토큰은 요청마다 새로 발급받아 헤더에 실어야 한다.

```bash
XSRF_TOKEN=$(curl -s http://localhost:8080/api/auth/csrf-token -b cookies.txt -c cookies.txt | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")

curl -i -X POST http://localhost:8080/api/admin/categories \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $XSRF_TOKEN" \
  -b cookies.txt \
  -d '{"name":"브랜드 필름","slug":"brand-film","displayOrder":1,"visible":true}'
```

## 4. 로그아웃 → Session 무효화 확인

로그아웃도 CSRF 보호 대상이라 토큰을 다시 발급받아야 한다.

```bash
XSRF_TOKEN=$(curl -s http://localhost:8080/api/auth/csrf-token -b cookies.txt -c cookies.txt | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")

curl -i -X POST http://localhost:8080/api/auth/logout \
  -H "X-XSRF-TOKEN: $XSRF_TOKEN" \
  -b cookies.txt

# 동일 쿠키로 재요청 시 401
curl -i http://localhost:8080/api/auth/me -b cookies.txt
```

## 5. Public API는 인증 없이 접근 확인

```bash
curl -i http://localhost:8080/api/public/works
curl -i http://localhost:8080/api/public/categories
```

## 6. 인증/인가 실패 확인

```bash
# 인증 없이 Admin API 호출 → 403 (CSRF 토큰이 없어 인증 여부 판단 전에 CsrfFilter가 먼저 차단)
curl -i -X POST http://localhost:8080/api/admin/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"test","slug":"test"}'

# 잘못된 자격 증명 → 401 (계정 미존재/비밀번호 오류/비활성 계정 모두 동일 응답, CSRF 토큰은 위 1번과 동일하게 선발급 필요)
XSRF_TOKEN=$(curl -s http://localhost:8080/api/auth/csrf-token -c wrong_cookies.txt | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")

curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $XSRF_TOKEN" \
  -b wrong_cookies.txt \
  -d '{"email":"wrong@douid.kr","password":"wrong"}'
```

## 서버 실행

```bash
cd apps/api
./gradlew bootRun
```

## 테스트 실행

```bash
./gradlew test
```
