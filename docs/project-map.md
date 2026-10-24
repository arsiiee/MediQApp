# Project Map

## Android (`app/`)
- `MainActivity.kt` — entry point, hosts Compose + bottom bar.
- `ui/theme/` — Color.kt, Type.kt, Spacing.kt, Shape.kt (design tokens; only place for raw color).
- `ui/components/` — MediQBottomBar (reference), MediQBackHeader, MediQNotice, MediQStatusMark.
- `ui/navigation/` — Routes.kt, MediQNavHost.kt.
- `ui/screens/` — one package per screen family:
  - `home/` — HomeScreen (reference implementation of token system).
  - `doctors/`, `doctordetails/` — list + detail, search empty state.
  - `booking/` — booking flow.
  - `appointments/`, `appointmentdetails/` — tracking + manage one booking.
  - `auth/` — SignIn + 4-step Register.
  - `messages/`, `secretarychat/` — secretary messaging.
  - `profile/` — account + health data.
  - `notifications/` — updates list.
- `ui/screens/*` follow pattern: `*Screen.kt`, `*UiState.kt`, `*States.kt`, `*Previews.kt`, `*Samples.kt`.

## Web frontend (`mediqweb/mediqweb-fe/`)
- Angular 21 app. `src/` components, `public/` assets. Config: angular.json, tsconfig*.json, .prettierrc.
- **Runtime wiring still pending** (no `provideHttpClient()` in `app.config.ts`; no `environment.ts`; no route guards; auth gateway mocked; area sessions backed by mock-data fixtures).
- **TypeScript client generated**: `npm run generate:ts-client` runs `openapi-typescript-codegen` against `server/src/main/resources/openapi/documentation.yaml` and writes `src/app/core/api/generated/` (models + `MediQClient`). `tsc --noEmit` passes for both `tsconfig.app.json` and `tsconfig.spec.json`. The generated client is not yet wired into the running app; see `docs/api-contract.md` for the Hono vs Ktor split.

## Web API (`mediqweb/mediqapp-api/`)
- Hono on Node (TypeScript). `src/` (index.ts entry).
- **Missing wiring** (minimal route surface): only `/health` and `/profile` routes exposed; Supabase middleware configured but underused; no endpoints for appointments, accounts, patients, doctors, schedules, messages, notifications, or password reset.

## Ktor server (`server/`)
- Standalone Gradle project. `build/install/server` runnable. Tests in `server/build/.../test`.

## Supabase (`supabase/`)
- Postgres schema/migrations. `supabase/migrations/20261024013521_initial_schema.sql` mirrors the Ktor entities (Flyway `V1__initial_schema.sql` DDL), applied to the linked Supabase project.

## Docs (`docs/`)
- `design-contract.md` — UI quality bar and increment order.
- `project-map.md` — this file.
- `api-contract.md` — all endpoints, request/response shapes, auth rules, Hono vs Ktor implementation status.
- `system-current-condition.md` — consolidated current-condition reference (build statuses, wiring gaps).
- `local-dev-setup.md` — Docker Compose + service-by-service local dev instructions.
- `production-deploy.md` — production deploy checklist (env vars, TLS, Supabase pooling, monitoring).
