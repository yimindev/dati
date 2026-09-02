# ==========================================
# Stage 1: Build Frontend and Documentation
# ==========================================
FROM node:22-alpine AS frontend-builder

WORKDIR /build

# Enable pnpm 9 to match lockfile
RUN corepack enable && corepack prepare pnpm@9.15.4 --activate

# Copy package files for dependency caching
COPY frontend/package.json frontend/pnpm-lock.yaml ./frontend/

# Install frontend dependencies
WORKDIR /build/frontend
RUN pnpm install --frozen-lockfile

# Copy frontend and docs source
WORKDIR /build
COPY frontend ./frontend
COPY docs ./docs

# Set PATH so docs build finds vitepress binaries when changing directory
ENV PATH="/build/frontend/node_modules/.bin:$PATH"

# Ensure root is treated as ESM for VitePress config resolution
RUN echo '{"type": "module"}' > /build/package.json

# Symlink node_modules so docs directory can resolve shared dependencies
RUN ln -s /build/frontend/node_modules /build/node_modules

# Build documentation (VitePress) and frontend (Vue 3 SPA)
WORKDIR /build/frontend
RUN cd ../docs/user-guide && /build/frontend/node_modules/.bin/vitepress build . \
    && cd /build/frontend && /build/frontend/node_modules/.bin/vue-tsc -b \
    && /build/frontend/node_modules/.bin/vite build

# ==========================================
# Stage 2: Build Backend Standalone Fat JAR
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /build

# Copy root pom and backend pom for dependency caching
COPY pom.xml ./
COPY backend/pom.xml ./backend/

# Copy backend source code
COPY backend ./backend

# Copy built frontend & docs dist into backend static resources
COPY --from=frontend-builder /build/frontend/dist /build/backend/app/src/main/resources/static

# Build Spring Boot Fat JAR
RUN mvn clean package -DskipTests -f backend/pom.xml

# ==========================================
# Stage 3: Production Runtime (JRE 21 Alpine)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Build-time app version (mirrors backend/pom.xml <version>)
ARG APP_VERSION=unknown
LABEL org.opencontainers.image.version="${APP_VERSION}"

WORKDIR /app

# Ensure directory for H2 embedded database file persistence
RUN mkdir -p /app/db

# Copy compiled JAR from builder stage (wildcard: decoupled from pom <version>)
COPY --from=backend-builder /build/backend/app/target/dati-app-*.jar app.jar

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8" \
    SERVER_PORT=8085

EXPOSE 8085

VOLUME ["/app/db"]

HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
