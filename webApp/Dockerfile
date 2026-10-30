# Stage 1: Build Compose Multiplatform Wasm WebApp using Gradle
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install bash and curl if needed
RUN apk add --no-cache bash curl

# Copy Gradle wrappers and build definitions
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ ./gradle/
COPY shared/ ./shared/
COPY webApp/ ./webApp/

# Build Wasm distribution
RUN chmod +x ./gradlew && ./gradlew :webApp:wasmJsBrowserDistribution --no-daemon

# Stage 2: Serve static files with lightweight Alpine Nginx
FROM nginx:alpine

# Copy custom Nginx configuration
COPY webApp/nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled web output
COPY --from=builder /app/webApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
