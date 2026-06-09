---
name: seucorre-backend-ia
description: Build and review the SeuCorre backend and AI pipeline in /desktop/seucorre. Use when changing Spring Boot controllers, services, domain rules, Flyway migrations, JWT auth, wearable integrations, AI prompts, providers, parsers, validation, or backend tests.
---

# SeuCorre Backend IA

Use this skill for backend and AI work in SeuCorre.

## Read first

Start with the smallest relevant set from:

- `/desktop/seucorre/.claude/ARCHITECTURE.md`
- `/desktop/seucorre/.claude/BUSINESS_RULES.md`
- `/desktop/seucorre/.claude/AI_GUIDELINES.md`
- `/desktop/seucorre/.claude/DECISIONS.md`

Then inspect the exact Java packages and tests involved in the task.

## Current backend shape

- Spring Boot monolith with domain-oriented packages
- PostgreSQL with Flyway
- Redis-backed infrastructure for rate limiting and cache support
- JWT auth
- AI providers behind `IAClient`
- Main domains: `usuario`, `treino`, `avaliacao`, `shared`, `infra`

## AI pipeline rules

- `PromptBuilder` builds context and output instructions
- `IAClient` isolates the provider
- providers may vary, business rules may not
- structured AI output must be parsed and validated
- critical rules stay in Java
- persistence only happens after the result is coherent

## Backend rules

- Keep controller methods thin.
- Put orchestration in application services.
- Keep domain rules in domain or explicit business logic classes.
- Prefer extending existing DTO and repository patterns over inventing new layers.
- Match schema changes with Flyway migrations.
- If the task changes behavior, update or add tests near the affected package.

## Common task workflow

1. Read the controller and service entry points.
2. Trace the domain objects and repositories involved.
3. Inspect related tests before editing.
4. Implement the change with minimal blast radius.
5. Validate compile-time behavior or run focused tests when practical.
6. Update `.claude` docs if rules or architecture changed.

## High-risk areas

- auth and token flow
- onboarding gating
- plan generation and parsing
- rule-of-10-percent safety logic
- check-in driven plan rewrite
- migrations that can desync domain and database

## Validation targets

Prefer targeted checks:

- focused JUnit tests around the changed package
- integration tests when controller or persistence behavior changes
- parser or provider tests when prompt/output behavior changes

If no test is run, report the gap clearly.
