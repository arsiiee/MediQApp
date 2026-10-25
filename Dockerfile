# Dockerfile for the MediQ Ktor server (Android-facing API)
# Multi-stage: builds the server with Gradle, runs on a slim JRE.
# Build context: repo root. Server Gradle build lives in server/.

# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY server/gradlew server/gradlew.bat ./
COPY server/gradle gradle
COPY server/build.gradle.kts server/settings.gradle.kts ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY server/src src
RUN ./gradlew installDist -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/install/mediq-server /opt/server
EXPOSE 8099
# PORT is injected by Render; MainKt.kt reads it and binds to 0.0.0.0.
CMD ["/opt/server/bin/mediq-server"]
