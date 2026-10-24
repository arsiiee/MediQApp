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
ENV DB_URL=jdbc:postgresql://postgres:5432/mediq
ENV DB_USER=mediq
ENV DB_PASSWORD=mediq_dev_password
ENV DB_DRIVER=org.postgresql.Driver
ENV JWT_SECRET=mediq-dev-secret-change-in-production
ENV CORS_ORIGINS=http://localhost:4200,http://localhost:3000
CMD ["/opt/server/bin/mediq-server"]
