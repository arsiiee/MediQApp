# Render.com Deployment — MediQ Multi-Service

Deploys the full MediQ stack on Render.com:

| Surface | Service type | Root dir | Port | Role |
|---------|-------------|----------|------|------|
| Ktor backend | Web service (Java) | `server/` | Render-injected | Android-facing API (port 8099 locally) |
| Hono BFF | Web service (Node) | `mediqweb/mediqapp-api/` | Render-injected | Web auth proxy + aggregations (port 3000 locally) |
| Angular frontend | Static site | `mediqweb/mediqweb-fe/` | CDN (80) | Web dashboard |
| PostgreSQL | Managed database | — | 5432 | Production data store for Ktor |

All services are defined in the root `render.yaml`. Connect the repo to Render, create a **Blueprint** from `render.yaml`, then configure the cross-service environment variables described below.

## 1. Prerequisites

- A Render account (free or paid)
- The MediQApp repo connected to Render via GitHub
- A Supabase project (for `SUPABASE_URL` / `SUPABASE_PUBLISHABLE_KEY` used by the Hono BFF)
- The `render.yaml` and `mediqweb/mediqweb-fe/render-build.sh` files committed and pushed

## 2. Step-by-Step Deployment

### Step 1 — Push the Render blueprint

```bash
git add render.yaml mediqweb/mediqweb-fe/render-build.sh docs/render-deploy.md
git commit -m "Add Render.com blueprint for all MediQ services"
git push origin main
```

### Step 2 — Create the Blueprint on Render

1. Log in to Render Dashboard → **New +** → **Blueprint**
2. Connect your GitHub repo: `MediQApp`
3. Render auto-detects `render.yaml`. Review the services:
   - `mediq-ktor-server` — Java web service, rootDir `server`
   - `mediq-hono-bff` — Node web service, rootDir `mediqweb/mediqapp-api`
   - `mediq-web-fe` — Static site, rootDir `mediqweb/mediqweb-fe`
   - `mediq-db` — PostgreSQL database
4. Click **Apply** (or **Create**)

### Step 3 — Configure environment variables

After the blueprint creates the services, set the cross-service variables in the Render Dashboard:

#### mediq-ktor-server (Ktor)

Go to the service → **Environment** and add:

| Variable | Value | Required | Notes |
|----------|-------|----------|-------|
| `DB_URL` | `jdbc:postgresql://<mediq-db-internal-host>:5432/mediq` | Yes | Get host from mediq-db → Connect → Internal connection string, prefix with `jdbc:` |
| `DB_USER` | (auto from mediq-db) | Yes | Use `fromDatabase` binding in render.yaml |
| `DB_PASSWORD` | (auto from mediq-db) | Yes | Use `fromDatabase` binding in render.yaml |
| `DB_DRIVER` | `org.postgresql.Driver` | No | Already set in render.yaml |
| `JWT_SECRET` | (auto-generated) | Yes | From `mediq-secrets` envGroup |
| `CORS_ORIGINS` | `https://mediq-web-fe.onrender.com` | Yes | Comma-separated, no wildcards. Add more origins as needed. |

> Encrypt sensitive values (`DB_PASSWORD`, `JWT_SECRET`) using Render's "Encrypt" toggle.

#### mediq-hono-bff (Hono)

| Variable | Value | Required | Notes |
|----------|-------|----------|-------|
| `KTOR_BASE_URL` | `https://mediq-ktor-server.onrender.com` | Yes | Must be HTTPS |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | Yes | From Supabase project settings |
| `SUPABASE_PUBLISHABLE_KEY` | `<supabase-anon-key>` | Yes | From Supabase project settings |
| `COOKIE_SECRET` | (auto-generated) | Yes | From `mediq-secrets` envGroup |
| `NODE_ENV` | `development` | No | Already set in render.yaml (keeps tsx installed) |

#### mediq-web-fe (Angular static site)

| Variable | Value | Required | Notes |
|----------|-------|----------|-------|
| `API_BASE_URL` | `https://mediq-ktor-server.onrender.com` | Yes | Injected into `environment.prod.ts` at build time |
| `BFF_BASE_URL` | `https://mediq-hono-bff.onrender.com` | Yes | Injected into `environment.prod.ts` at build time |

### Step 4 — Deploy

Render builds on every push to the connected branch:

1. **Ktor**: `./gradlew installDist -x test --no-daemon` in `server/`, then starts `./build/install/mediq-server/bin/mediq-server`. Flyway migrations run at startup; demo data is seeded if tables are empty.
2. **Hono BFF**: `npm install`, then `npx tsx src/index.ts`.
3. **Angular**: `render-build.sh` runs `npm install`, substitutes `API_BASE_URL`/`BFF_BASE_URL` into `environment.prod.ts`, then `npm run build` outputs to `dist/mediqapp-fe`.

Verify each deployment:

```bash
curl https://mediq-ktor-server.onrender.com/health
# → {"status":"ok"}

curl https://mediq-hono-bff.onrender.com/health
# → {"status":"ok"}

curl https://mediq-web-fe.onrender.com
# → Angular app HTML
```

## 3. Connecting the Android App

After deployment, update the Android client's base URL to point at Render:

- Local dev: `http://127.0.0.1:8099/` (requires `adb reverse tcp:8099 tcp:8099`)
- Production: `https://mediq-ktor-server.onrender.com/`

The Android `RetrofitClient` base URL is configured in `app/src/main/java/com/example/mediq/data/api/`. Update it for release builds against the Render URL.

## 4. Environment Variable Reference

### Ktor server (`server/`)

| Variable | Source | Default | Production guidance |
|----------|--------|---------|---------------------|
| `PORT` | Render-injected | 8099 | Do not set manually |
| `DB_URL` | render.yaml (user-set) | H2 in-memory | `jdbc:postgresql://<host>:5432/mediq` |
| `DB_USER` | render.yaml (fromDatabase) | `sa` | mediq-db user |
| `DB_PASSWORD` | render.yaml (fromDatabase) | `""` | mediq-db password (encrypt in Render) |
| `DB_DRIVER` | render.yaml | `org.h2.Driver` | `org.postgresql.Driver` |
| `JWT_SECRET` | mediq-secrets envGroup | dev secret | Auto-generated; min 256 bits; rotate periodically |
| `CORS_ORIGINS` | render.yaml (user-set) | localhost origins | Production frontend domain(s) only |

### Hono BFF (`mediqweb/mediqapp-api/`)

| Variable | Source | Default | Production guidance |
|----------|--------|---------|---------------------|
| `PORT` | Render-injected | 3000 | Do not set manually |
| `KTOR_BASE_URL` | render.yaml (user-set) | `http://localhost:8099` | Must be HTTPS in production |
| `SUPABASE_URL` | render.yaml (user-set) | Supabase project URL | From Supabase settings |
| `SUPABASE_PUBLISHABLE_KEY` | render.yaml (user-set) | Supabase anon key | From Supabase settings |
| `COOKIE_SECRET` | mediq-secrets envGroup | dev secret | Auto-generated; encrypt in Render |
| `NODE_ENV` | render.yaml | `development` | Keeps tsx devDependency at runtime |

### Angular frontend (`mediqweb/mediqweb-fe/`)

| Variable | Source | Default | Production guidance |
|----------|--------|---------|---------------------|
| `API_BASE_URL` | render.yaml (user-set) | `https://api.mediq.app` | Substituted at build time into `environment.prod.ts` |
| `BFF_BASE_URL` | render.yaml (user-set) | `https://bff.mediq.app` | Substituted at build time into `environment.prod.ts` |

## 5. Port Handling

- **Ktor**: `MainKt.kt` resolves port with priority: `PORT` env → `MEDIQ_PORT` env → `ktor.deployment.port` system property → fallback `8099`. Binds to `0.0.0.0`. Render injects `PORT` automatically.
- **Hono BFF**: `src/index.ts` reads `PORT` env var. Render injects `PORT` automatically.
- **Angular**: Served from Render's CDN on port 80 (static site). No `PORT` needed.

## 6. Health Checks

| Service | Endpoint | Expected |
|---------|----------|----------|
| Ktor | `GET /health` | `200 { "status": "ok" }` |
| Hono BFF | `GET /health` | `200 { "status": "ok" }` |

Render polls these for health-check gating and auto-deploy.

## 7. Files Changed / Added

- `render.yaml` — Render blueprint: database, envGroups, and all three services
- `mediqweb/mediqweb-fe/render-build.sh` — Static-site build script (npm install + env substitution + `ng build`)
- `gradlew` / `server/gradlew` — Normalized to Unix (LF) line endings for Render's Linux build environment

## 8. Related Files

- `docs/production-deploy.md` — General production checklist (TLS, Supabase pooling, security)
- `docs/api-contract.md` — Full API endpoint reference
- `server/src/main/resources/application.conf` — Ktor config keys and defaults
- `server/Dockerfile` — Precedent for Unix line-ending normalization (`sed -i 's/\r$//' gradlew`)
