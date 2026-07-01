---
name: seucorre-frontend-mvp
description: Work on the active SeuCorre frontend MVP in /desktop/seucorre/src/front. Use when changing React/Vite routes, auth, onboarding, plan generation, history, notifications, wearables, paywall, or frontend/backend integration while following the local .claude guides and Obsidian project maps.
---

# SeuCorre Frontend MVP

Use this skill for frontend work in SeuCorre when repository-local product and route constraints matter.

## Read first

Start with the smallest relevant set from:

- `/desktop/seucorre/.claude/README.md`
- `/desktop/seucorre/.claude/ARCHITECTURE.md`
- `/desktop/seucorre/.claude/FRONTEND_GUIDELINES.md`
- `/desktop/seucorre/.claude/BUSINESS_RULES.md` when the task changes auth, onboarding, plans, wearables, notifications, or premium behavior
- Obsidian references when flow context matters:
  - `/desktop/Obsidian Vault/Projetos/SeuCorre/05 Frontend/Mapa do Frontend.md`
  - `/desktop/Obsidian Vault/Projetos/SeuCorre/05 Frontend/Telas, Estados e Integracoes.md`
  - `/desktop/Obsidian Vault/Projetos/SeuCorre/01 Visao do Produto/Jornadas e Fluxos Principais.md`

## Active frontend map

- Route source of truth: `/desktop/seucorre/src/front/src/App.jsx`
- Auth and onboarding gating: `/desktop/seucorre/src/front/src/lib/AuthContext.jsx`
- API wrapper, token refresh, and normalizers: `/desktop/seucorre/src/front/src/lib/api.js`
- Active route tree: `/desktop/seucorre/src/front/src/page/backend`
- Parallel legacy tree: `/desktop/seucorre/src/front/src/page`
- Paywall currently lives in `/desktop/seucorre/src/front/src/page/Paywall.jsx`
- Reusable onboarding UI: `/desktop/seucorre/src/front/src/components/onboarding`
- Layout shell: `/desktop/seucorre/src/front/src/components/layout/AppLayout.jsx`

## Confirmed routes

- `/`, `/entrar`, `/cadastro`, `/onboarding`, `/gerar-plano`
- `/treino/:planoId/:sessionIdx`, `/check-in`, `/historico`
- `/wearables`, `/notificacoes`, `/paywall`
- `/plano`, `/ia`, `/progresso`, `/perfil`

## Core journey

- Visitor sees landing or auth routes
- Login or signup stores tokens and triggers `/api/usuarios/me` in `AuthContext`
- Authenticated user without onboarding is redirected to `/onboarding`
- Completed onboarding unlocks `/gerar-plano` and the private app routes
- Ongoing user flow continues through plan, workout execution, check-in, progress, history, wearables, notifications, and paywall

## Guardrails

- Treat `src/page/backend` as the active route tree. Do not duplicate or fork behavior into both trees unless consolidation is the task.
- Preserve auth redirect, onboarding redirect, and protected-route behavior unless the task explicitly changes product flow.
- Keep HTTP calls in `lib/api.js`; do not add page-local `fetch` when the API layer already owns the contract.
- Preserve the request-wrapper behavior that retries a `401` once through refresh token before failing.
- Keep payload normalization in `lib/api.js` or a dedicated helper, not inside view components.
- Every data-driven screen should cover loading, error, empty, and protected-redirect states.
- Frontend validation should be fast and specific, but the backend remains the source of truth.
- Notifications, paywall, and wearables may be only partially wired in the backend. UI copy should say that clearly instead of implying full support.

## Common workflows

### Auth or onboarding change

1. Read `App.jsx`, `AuthContext.jsx`, `lib/api.js`, the relevant page, and any helper such as `lib/user.js`.
2. Trace navigation for visitor, authenticated user, and authenticated user without onboarding.
3. Validate field errors, submit state, and final redirect.
4. Run `npm run lint`, `npm run typecheck`, and `npm run build` from `/desktop/seucorre/src/front`.

### Screen with backend data

1. Read the page plus matching API functions and normalizers in `lib/api.js`.
2. Check the backend controller before inventing endpoint or payload shape.
3. Keep text aligned with the real MVP status when the backend feature is incomplete.
4. Prefer minimal UI changes over route or state rewrites.

### Route or layout change

1. Start from `App.jsx`.
2. Check whether the page is standalone or nested under `AppLayout`.
3. Preserve mobile-first layout and auth gating.

## Validation

- Default checks: `npm run lint`, `npm run typecheck`, `npm run build`
- For auth or onboarding changes, also sanity-check login -> onboarding -> gerar-plano navigation
- If validation cannot run, report the exact gap

## Review mode

When reviewing frontend changes, prioritize:

- auth or onboarding regressions
- route protection leaks
- frontend and backend contract mismatches
- missing loading, error, or empty states
- duplicated page logic between `src/page` and `src/page/backend`
