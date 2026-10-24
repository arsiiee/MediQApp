# Local Dev Setup

This document describes how to run the full MediQ stack locally using Docker Compose, or service-by-service without Docker.

## Prerequisites

- Docker & Docker Compose (for the containerized setup)
- JDK 17 (for Ktor server)
- Node.js 20 + npm (for Hono API and Web FE)
- Android SDK + emulator or physical device (for Android app)
- `adb` (for Android port forwarding)

## Option 1: Docker Compose (all services)

From the repo root:

```bash
docker-compose up --build
```

This starts:

| Service | Port | Description |
|---------|------|-------------|
| postgres | 5432 | Local Postgres database for Ktor |
| ktor-server | 8099 | Ktor API (Android-facing) |
| hono-bff | 3000 | Hono BFF (Web API, proxies to Ktor) |
| web-fe | 4200 | Angular web frontend |

### Environment variables

The `docker-compose.yml` sets sensible defaults for local development. Override via `.env` or `docker-compose.override.yml`:

| Variable | Default | Service |
|----------|---------|---------|
| `db.url` | `jdbc:postgresql://postgres:5432/mediq` | Ktor |
| `db.user` | `mediq` | Ktor |
| `db.password` | `mediq_dev_password` | Ktor |
| `jwt.secret` | `mediq-dev-secret-change-in-production` | Ktor |
| `cors.origins` | `http://localhost:4200,http://localhost:3000` | Ktor |
| `PORT` | `3000` | Hono BFF |
| `KTOR_BASE_URL` | `http://ktor-server:8099` | Hono BFF |
| `SUPABASE_URL` | cloud project | Hono BFF (`/profile`) |
| `SUPABASE_PUBLISHABLE_KEY` | cloud project | Hono BFF (`/profile`) |

### Supabase

The linked Supabase project (`wdoyhtjpjhmhsegdllvd`) is cloud-provisioned and used by the Hono BFF `/profile` endpoint. Its credentials are in `mediqweb/mediqapp-api/.env` (not committed). The Ktor server uses the local Postgres container by default; point `db.url` at Supabase if desired.

## Option 2: Service-by-service (no Docker)

### Ktor server

```bash
cd server
./gradlew.bat run        # listens on 8099 (H2 default)
# Or with Postgres:
db.url=jdbc:postgresql://localhost:5432/mediq ./gradlew.bat run
```

### Hono BFF

```bash
cd mediqweb/mediqapp-api
cp .env.example .env     # add SUPABASE_URL / SUPABASE_PUBLISHABLE_KEY
npx tsx src/index.ts     # listens on 3000
# Or with compiled JS:
npx tsc && node --env-file .env src/index.js
```

### Web frontend

```bash
cd mediqweb/mediqweb-fe
npm ci
npm start                # ng serve on 4200
```

The web FE currently runs against mock data by default (`USE_HTTP_API` defaults to `false` in tests, `true` in the app via `app.config`). Point it at the running Ktor/Hono APIs via environment configuration.

### Android app

```bash
cd ..                    # repo root
./gradlew :app:assembleDebug
adb reverse tcp:8099 tcp:8099   # emulator → host port forwarding
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.mediq/.MainActivity
```

The `adb reverse` step is required — without it, the emulator cannot reach `127.0.0.1:8099` and sign-in reports "We could not reach MediQ".

### Verifying all services

```bash
curl http://localhost:8099/health          # → {"status":"ok"}
curl http://localhost:3000/health          # → {"status":"ok"}
curl -X POST http://localhost:3000/auth/sign-in -H "Content-Type: application/json" -d '{"username":"demo_patient","password":"demo12345"}'
```

Demo account: `demo_patient` / `demo12345`

## CI

See `.github/workflows/ci.yml` for the GitHub Actions pipeline covering Ktor tests, Web FE tests (with Chrome), Hono API tests, Android tests/build, and OpenAPI spec validation.
