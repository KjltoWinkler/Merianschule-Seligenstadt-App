# Stage 1: Build application distribution using Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and project definition files
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/
COPY shared/ ./shared/
COPY server/ ./server/

# Make gradlew executable and build install distribution for :server
RUN chmod +x ./gradlew && ./gradlew :server:installDist --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for container health checks
RUN apk add --no-cache curl

# Create directory for persistent SQLite database and JSON files
RUN mkdir -p /app/data

# Copy the built distribution from builder stage
COPY --from=builder /app/server/build/install/server/ /app/

# Set default environment variables
ENV PORT=8080
ENV HOST=0.0.0.0
ENV DATA_DIR=/app/data
ENV DB_PATH=/app/data/merianschule.db
ENV ADMIN_SECRET_KEY=merian-admin-secret-2026
ENV JWT_SECRET=merianschule-seligenstadt-secret-key-2026-safe-production

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["/app/bin/server"]
