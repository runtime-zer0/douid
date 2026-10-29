# DOUID Web

Next.js App Router 기반 DOUID 프론트엔드

## 요구 사항

- Node.js 24 LTS
- npm

## 시작하기

```bash
npm install
cp .env.example .env.local
npm run dev
```

http://localhost:3000 에서 확인

## 스크립트

| 명령                | 설명                                 |
| ------------------- | ------------------------------------ |
| `npm run dev`       | 개발 서버 (Turbopack)                |
| `npm run build`     | 프로덕션 빌드                        |
| `npm run start`     | 프로덕션 서버                        |
| `npm run lint`      | ESLint                               |
| `npm run typecheck` | 라우트 타입 생성 후 TypeScript 검사  |
| `npm run test`      | Vitest watch                         |
| `npm run test:run`  | Vitest 1회 실행                      |
| `npm run test:e2e`  | Playwright E2E (개발 서버 자동 실행) |

Playwright 브라우저가 없다면 `npx playwright install chromium`을 먼저 실행

## 구조

```text
src/
├── app/       # 라우팅, Root Layout, Provider, 전역 CSS
├── features/  # 기능 단위 컴포넌트와 로직
├── shared/    # 여러 기능이 공유하는 UI와 유틸
└── test/      # Vitest setup, MSW 서버
e2e/           # Playwright 시나리오
```
