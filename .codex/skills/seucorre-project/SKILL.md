---
name: seucorre-project
description: Work on the SeuCorre SaaS in /desktop/seucorre. Use when implementing or reviewing cross-cutting changes, backend features, AI flows, domain rules, tests, documentation, or frontend/backend integration that must follow the project guidance in .claude.
---

# SeuCorre Project

Use this skill for general work in the SeuCorre project.

For UI-heavy frontend work, also use `seucorre-frontend` when it is available.

## Mandatory orientation

Before editing, read only the relevant docs from `/desktop/seucorre/.claude`:

- `README.md` for operating rules
- `ARCHITECTURE.md` for stack and system shape
- `BUSINESS_RULES.md` for product behavior
- `AI_GUIDELINES.md` for prompt and validation guardrails
- `FRONTEND_GUIDELINES.md` for UI and UX constraints
- `DECISIONS.md` for accepted technical choices and open questions

Load only what the task needs. Do not pull the entire project into context by default.

## Repository map

- Backend: `/desktop/seucorre/src/main/java/com/seucorre`
- Backend config and migrations: `/desktop/seucorre/src/main/resources`
- Frontend: `/desktop/seucorre/src/front`
- Project docs for humans and AI: `/desktop/seucorre/.claude`
- Diagrams and supporting artifacts: `/desktop/arquivosdoprojeto`

## Project guardrails

- Treat the backend as the source of truth for business rules.
- Do not move critical rules into prompts only.
- Preserve onboarding gating and auth flow unless the task explicitly changes them.
- Do not invent backend endpoints without checking controllers and services first.
- Prefer small, coherent changes over broad rewrites.
- Never touch unrelated modified files in this repository.

## Domain map

- `usuario`: auth, profile, onboarding, wearable devices
- `treino`: plan generation, sessions, records, AI plan parsing
- `avaliacao`: weekly check-in, progress, risk analysis
- `infra`: security, IA providers, cache, events, scheduler, notifications
- `shared`: enums, events, exceptions, value objects

## Workflow

### Cross-cutting change

1. Map the user flow or backend flow first.
2. Identify the source-of-truth files before editing.
3. Update the smallest viable set of files.
4. Validate affected layers.
5. Update `.claude` docs if architecture, rules, or process changed.

### Backend or AI change

Read the relevant controller, application service, domain class, repository, DTOs, tests, and provider code before editing. Keep provider abstractions intact and validate AI output before persistence.

### Frontend and API integration change

Read `App.jsx`, auth context, API client, and the relevant pages/components. Preserve auth, onboarding, and request-wrapper behavior.

## Validation

Use the narrowest useful validation for the files you changed:

- Backend: compile or run focused tests when practical
- Frontend: lint, typecheck, and build when practical
- If you cannot run validation, say exactly what was not run

## Review mode

If asked for a review, prioritize:

- regressions in product flow
- broken auth or onboarding gating
- backend/frontend contract mismatches
- AI safety and validation gaps
- missing tests on critical behavior
