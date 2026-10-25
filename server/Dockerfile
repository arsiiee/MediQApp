# Multi-stage Dockerfile for the Ktor server (Android-facing API)
# Works on Linux (Railway, Docker Compose, etc.)

# Stage 1: Build the application distribution
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
# Gradle wrapper and config
COPY gradlew gradlew.bat settings.gradle.kts ./
COPY gradle gradle
# Ensure Unix line endings & exec permission on the shell wrapper
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
COPY build.gradle.kts ./
# Cache dependency download
RUN ./gradlew dependencies --no-daemon || true
# Copy source and build
COPY . .
RUN ./gradlew installDist -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/install/mediq-server /opt/server
EXPOSE 8099
# Production/Supabase: set DB_URL, DB_USER, DB_PASSWORD, DB_DRIVER at deploy time.
# Local dev defaults to H2 (no env vars needed).
# Railway injects PORT — Ktor reads it from application.conf or env.
# Configure the server at deploy time via environment variables:
#   DB_URL, DB_USER, DB_PASSWORD, DB_DRIVER   (PostgreSQL/Supabase; Railway vars,
#   Docker -e flags, Kubernetes secrets, etc.)
#   JWT_SECRET                                (set a strong secret at deploy time)
#   CORS_ORIGINS                              (allowed frontend origins)
#
# Local H2 dev: no DB env vars are needed — H2 is the default.
# The server auto-runs Flyway migrations + DatabaseSeeder on startup.
CMD ["/opt/server/bin/mediq-server"]
