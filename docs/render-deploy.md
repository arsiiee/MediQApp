# Render.com Deployment — MediQ Ktor Backend

Deploys the Ktor server (`server/`) as the Android-facing API on Render.com.

## 1. Backend Current Status

| Item | Status |
|------|--------|
| **Backend** | Ktor 3.2.3 (Kotlin/JVM, Netty), port 8099 |
| **Build** | `./gradlew build -x test` — **green** in `server/` |
| **Runtime** | Running locally on port 8099 |
| **Health check** | `GET /health` → `200 { "status": "ok" }` |
| **Auth (demo)** | `demo_patient` / `demo12345` → `200` JWT issued |
| **Auth guard** | `/doctors` → `401` (bearer auth mounted, as expected) |
| **Routes** | Full contract implemented per `docs/api-contract.md` (auth, profile, doctors, appointments, messages, notifications, admin, secretary, specializations, patients) |
| **Data layer** | JDBC repositories (HikariCP + Flyway) wired in `MainKt.kt`; H2 dev default, Postgres/Supabase configurable via env |
| **Migrations** | Flyway `V1__initial_schema.sql` runs at startup from `db/migration/` |
| **Tests** | No test sources under `server/src/test/` (build configured, `test` task exists but is empty) |
| **Demo account** | `demo_patient` / `demo12345` (seeded by `DatabaseSeeder.seedIfEmpty()`) |

> The Hono BFF (`mediqweb/mediqapp-api/`) is a separate, minimal surface (`/health`, `/profile` only). It is **not** part of this Render deployment. The Ktor server is the canonical API for the Android app.

## 2. Prerequisites

- A Render account (free or paid)
- The MediQApp repo connected to Render via GitHub
- A PostgreSQL database (Render Postgres, Supabase, or external) for production data
- A strong `JWT_SECRET` (min 256 bits)

## 3. Step-by-Step Deployment

### Step 1 — Push `render.yaml` to GitHub

The `render.yaml` file at the repo root defines the web service. Commit and push it:

```bash
git add render.yaml
git commit -m "Add Render.com deployment config for Ktor backend"
git push origin main
```

### Step 2 — Create the Web Service on Render

1. Log in to Render Dashboard → **New +** → **Web Service**
2. Connect your GitHub repo: `MediQApp`
3. Render auto-detects `render.yaml`. Review the settings:
   - **Runtime:** Java
   - **Root directory:** `server`
   - **Build command:** `./gradlew installDist -x test --no-daemon`
   - **Start command:** `./build/install/mediq-server/bin/mediq-server`
   - **Health check path:** `/health`
   - **Plan:** `starter` (always-on) or `free` (spins down after inactivity)
4. Click **Create Web Service**

### Step 3 — Configure Environment Variables

In the Render Dashboard, go to your service → **Environment** and add:

| Variable | Value | Required |
|----------|-------|----------|
| `DB_URL` | `jdbc:postgresql://<host>:<port>/<db>` | Yes |
| `DB_USER` | `<db-user>` | Yes |
| `DB_PASSWORD` | `<db-password>` | Yes |
| `DB_DRIVER` | `org.postgresql.Driver` | No (default set in render.yaml) |
| `JWT_SECRET` | `<strong-random-secret>` | Yes (auto-generated if left blank) |
| `CORS_ORIGINS` | `https://<frontend-domain>.onrender.com` | Yes |

> Never commit secrets. Use Render's encrypted env var store (toggle "Encrypt" on sensitive values like `DB_PASSWORD` and `JWT_SECRET`).

### Step 4 — Attach a Database (Production)

If using **Render Postgres**:

1. Render Dashboard → **New +** → **PostgreSQL**
2. Copy the internal database URL (e.g., `jdbc:postgresql://<internal-host>:5432/<db>`)
3. Add it as `DB_URL` on the web service, along with `DB_USER` and `DB_PASSWORD`

If using **Supabase**:

1. Get the connection string from Supabase → Settings → Database
2. Use the **connection pooler** URL (port 6543) for server deployments
3. Set `DB_URL`, `DB_USER`, `DB_PASSWORD` accordingly
4. Recommended HikariCP sizing for Supabase: `maximumPoolSize: 5-10`, `maxLifetime: 1800000` (30 min, below Supabase idle timeout)

### Step 5 — Deploy

Render builds on every push to the connected branch. The build:

1. Runs `./gradlew installDist -x test --no-daemon` in `server/`
2. Produces `build/install/mediq-server/`
3. Starts `./build/install/mediq-server/bin/mediq-server`
4. Runs Flyway migrations at startup
5. Seeds demo data if tables are empty

Verify the deployment:

```bash
curl https://mediq-ktor-server.onrender.com/health
# → {"status":"ok"}
```

## 4. Environment Variable Reference

| Variable | Source | Default | Production guidance |
|----------|--------|---------|---------------------|
| `PORT` | Render-injected | 8099 | Do not set manually |
| `DB_URL` | `application.conf` → env | H2 in-memory | Set to Supabase/Postgres JDBC URL |
| `DB_USER` | `application.conf` → env | `sa` | Your DB user |
| `DB_PASSWORD` | `application.conf` → env | `""` | Your DB password (encrypt in Render) |
| `DB_DRIVER` | `application.conf` → env | `org.h2.Driver` | `org.postgresql.Driver` for Postgres |
| `JWT_SECRET` | `application.conf` → env | `mediq-dev-secret-change-in-production` | **Change in production**; min 256 bits |
| `CORS_ORIGINS` | `application.conf` → env | `http://localhost:4200,http://localhost:3000` | Production frontend domain(s) only, no wildcards |

`application.conf` (`server/src/main/resources/application.conf`) documents all config keys. Environment variables take precedence over the conf file.

## 5. Port Handling

Render injects the `PORT` environment variable. The server resolves it in `MainKt.kt` with this priority:

1. `System.getenv("PORT")` ← Render sets this automatically
2. `System.getenv("MEDIQ_PORT")`
3. `System.getProperty("ktor.deployment.port")`
4. Fallback: `8099`

The server binds to `0.0.0.0` (all interfaces), which is required for container/platform deployment.

## 6. Health Check

Render polls `GET /health` on the assigned port. The endpoint returns `200 { "status": "ok" }` with no auth required. Use this for Render's built-in health checks and auto-deploy gating.

## 7. Free Tier Limitations

If you choose the **free** plan:

- The service spins down after ~15 minutes of inactivity
- First request after spin-down incurs a cold start (30+ seconds)
- Android sign-in may report "We could not reach MediQ" during cold start
- 512 MB RAM, shared CPU, single instance

For a healthcare app where the Android client needs reliable sign-in, the **starter plan** (~$7/month, always-on) is recommended.

## 8. Connecting the Android App

After deployment, update the Android client's base URL to point at Render:

- Local dev: `http://127.0.0.1:8099/` (requires `adb reverse tcp:8099 tcp:8099`)
- Production: `https://mediq-ktor-server.onrender.com/`

The Android `RetrofitClient` base URL is configured in `app/src/main/java/com/example/mediq/data/api/`. Update it for release builds against the Render URL.

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Build fails | Gradle wrapper line endings on Windows | `render.yaml` runs on Linux; ensure `gradlew` has Unix line endings (already handled by Dockerfile precedent) |
| `502 Bad Gateway` | Start command wrong or binary missing | Verify `startCommand: ./build/install/mediq-server/bin/mediq-server` and `rootDir: server` |
| `/health` returns non-200 | DB connection failing | Check `DB_URL`/`DB_USER`/`DB_PASSWORD`; Flyway migrations may be blocked |
| Sign-in returns 500 | `JWT_SECRET` too weak or missing | Set a strong random `JWT_SECRET` (min 256 bits) |
| CORS errors from frontend | `CORS_ORIGINS` missing frontend domain | Add the Render frontend URL to `CORS_ORIGINS` |
| Service spins down | Free plan inactivity | Upgrade to starter plan or keep the service warmed up |

## 10. Related Files

- `render.yaml` — Render service definition (this directory root)
- `server/build.gradle.kts` — Ktor Gradle build (application plugin, `installDist` task)
- `server/src/main/kotlin/com/example/mediq/server/MainKt.kt` — entry point, port resolution
- `server/src/main/resources/application.conf` — config keys and defaults
- `docs/api-contract.md` — full API endpoint reference
- `docs/production-deploy.md` — general production checklist (TLS, Supabase pooling, security)
- `docs/system-current-condition.md` — consolidated build/status reference for all surfaces
