# Production Deploy Checklist

## 1. Environment Variables & Secrets

Never commit secrets. Use a secrets manager (AWS Secrets Manager, GCP Secret Manager, HashiCorp Vault, or your platform's native secrets).

### Ktor server (`server/`)

| Variable | Production value | Notes |
|----------|-----------------|-------|
| `db.url` | `jdbc:postgresql://<supabase-host>:5432/postgres` | Supabase Postgres connection string |
| `db.user` | `<supabase-db-user>` | Supabase database user |
| `db.password` | `<supabase-db-password>` | From secrets manager |
| `jwt.secret` | `<strong-random-secret>` | Min 256 bits, rotate periodically |
| `cors.origins` | `https://<web-app-domain>` | No wildcards in production |
| `rate.limit.requests-per-minute` | `60` | Adjust based on traffic |

### Hono BFF (`mediqweb/mediqapp-api/`)

| Variable | Production value | Notes |
|----------|-----------------|-------|
| `PORT` | `3000` | Or platform-assigned port |
| `KTOR_BASE_URL` | `https://<ktor-domain>` | Must be HTTPS |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | Supabase project URL |
| `SUPABASE_PUBLISHABLE_KEY` | `<supabase-anon-key>` | From secrets manager |

### Web frontend (`mediqweb/mediqweb-fe/`)

| Variable | Production value | Notes |
|----------|-----------------|-------|
| API base URL | `https://<ktor-domain>` | Point at Ktor, not localhost |
| BFF base URL | `https://<hono-domain>` | Point at Hono, not localhost |

## 2. TLS / HTTPS

- Terminate TLS at the load balancer / ingress (ALB, Cloudflare, nginx, etc.)
- Enforce HTTPS everywhere — redirect HTTP → HTTPS
- Use certificates from Let's Encrypt or your CA
- Set `secure: true` on cookies in production (Hono `setCookie` currently uses `secure: false` — change for prod)
- Enable HSTS header

### Hono cookie fix for production

In `mediqweb/mediqapp-api/src/index.ts`, the `setCookie` calls use `secure: false`. For production, set:

```ts
setCookie(c, "refresh_token", refreshToken, {
    httpOnly: true,
    sameSite: "Lax",
    path: "/",
    secure: true,  // <-- change to true in production
});
```

## 3. Supabase Connection Pooling

Ktor uses HikariCP. For production against Supabase:

| HikariCP setting | Recommended value | Reason |
|-----------------|-------------------|--------|
| `maximumPoolSize` | `5-10` | Supabase has connection limits; don't exhaust them |
| `minimumIdle` | `2-5` | Keep warm connections |
| `connectionTimeout` | `30000` | 30s |
| `idleTimeout` | `600000` | 10 min |
| `maxLifetime` | `1800000` | 30 min (below Supabase idle timeout) |

Consider using Supabase's connection pooler (port 6543) instead of direct Postgres (port 5432) for serverless/edge deployments.

Run Flyway migrations as part of deployment (not at app startup in high-availability setups):

```bash
cd server
./gradlew.bat flywayMigrate -Ddb.url=... -Ddb.user=... -Ddb.password=...
```

## 4. Monitoring & Logging

### Ktor server
- Enable `CallLogging` (already installed in `MainKt.kt`) — configure log level to `INFO` in prod
- Send logs to a central system (CloudWatch, Datadog, ELK, etc.)
- Monitor: request latency, error rate (5xx), auth failures, rate limit hits
- Health endpoint: `GET /health` — configure load balancer health checks against it

### Hono BFF
- Enable request logging (Hono `logger()` middleware)
- Monitor: proxy error rate to Ktor, cookie set/read failures
- Health endpoint: `GET /health`

### Web frontend
- Monitor: JavaScript errors, failed API calls, sign-in failures
- Use `ToastService` to surface API errors to users

## 5. Security

- **JWT**: Rotate `jwt.secret` regularly; set short access token expiry (e.g., 15 min); longer refresh token expiry (e.g., 30 days)
- **CORS**: Explicit allow-list of production domains only
- **Rate limiting**: Ktor has global rate limiting (100/min in dev) — tune for production traffic patterns
- **Headers**: Set `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy`
- **Database**: Use least-privilege Supabase roles; don't use the service role key in client-facing code

## 6. Database Migrations

- Ktor uses Flyway with migrations in `server/src/main/resources/db/migration/`
- Supabase schema mirrored in `supabase/migrations/`
- Run migrations before deploying new code versions
- Test migrations against a staging copy of production data
- Never skip `V1__initial_schema.sql` — it's the foundation

## 7. Pre-deployment Checklist

- [ ] All secrets in secrets manager, not in code or `.env`
- [ ] TLS certificates provisioned and auto-renewing
- [ ] `cors.origins` set to production domains only
- [ ] Hono `secure: true` on cookies
- [ ] HikariCP pool sized for Supabase connection limits
- [ ] Flyway migrations tested against staging data
- [ ] Load balancer health checks pointing at `/health` endpoints
- [ ] Log aggregation configured
- [ ] Error alerting configured (5xx rate, auth failures)
- [ ] Android APK built with release signing config
- [ ] Web FE built for production (`npm run build`)
- [ ] `USE_HTTP_API` set to `true` in web FE production config

## 8. Rollback Plan

- Keep previous Docker image tags available
- Database migrations are additive only (no destructive changes without a rollback script)
- Feature flags for major new functionality if available
