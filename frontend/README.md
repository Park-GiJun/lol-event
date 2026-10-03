# lol-event-fe

내전 통계 사이트의 화면. React 19 + Vite + TanStack Query.

```bash
pnpm install
pnpm dev      # http://localhost:8080 — /api 는 vite proxy 가 백엔드로 넘긴다
pnpm lint     # prettier --check + eslint
pnpm test     # vitest
pnpm build    # tsc -b + vite build
```

백엔드를 띄우지 않고 운영 데이터를 보려면 `.env.local` 에 `VITE_DEV_BE=https://api.lol.gijun.net` 을 넣는다
(`.env.example` 참고).

구조와 규칙은 저장소 루트의 `CLAUDE.md` — "프론트 구조" 에 있다.
