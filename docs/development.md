# Local Development Guide

[English](development.md) | [简体中文](development_zh.md)

This guide walks you through setting up and running DatI locally for development and debugging.

---

## Prerequisites & Dependencies

### Base Environment
- **Java 21**
- **Maven 3.9+**
- **Node.js 20+** & **pnpm 10+**

### External Dependency: Elasticsearch 8.x
- **Version Requirement**: The semantic search module is natively built on the Elasticsearch 8.x Java Client.
- **Analyzers (configurable, IK optional)**: The semantic index analyzers are configured via `DATI_ELASTICSEARCH_INDEX_ANALYZER` / `DATI_ELASTICSEARCH_SEARCH_ANALYZER`, defaulting to `standard` (built into Elasticsearch, works on any instance). For Chinese word-level segmentation use `ik_max_word` / `ik_smart` (requires the IK plugin).
  - Analyzers only apply when the `semantic_search` index is **first created**; after changing the configuration, delete that index and restart the application — data is rebuilt on the next sync.
- **Quick Local Setup**:
  - Default (no Chinese segmentation needed, vanilla image):
    ```bash
    docker run -d --name dati-es-dev -p 9200:9200 \
      -e "discovery.type=single-node" \
      -e "xpack.security.enabled=false" \
      -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
      elasticsearch:8.18.7
    ```
  - Chinese word-level segmentation: use the pre-bundled image with the IK plugin (matches the `docker-compose.yml` default) and inject the analyzer configuration when starting the backend:
    ```bash
    docker run -d --name dati-es-dev -p 9200:9200 \
      -e "discovery.type=single-node" \
      -e "xpack.security.enabled=false" \
      -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
      davyinsa/elasticsearch-ik:8.18.7
    
    DATI_ELASTICSEARCH_INDEX_ANALYZER=ik_max_word \
      DATI_ELASTICSEARCH_SEARCH_ANALYZER=ik_smart \
      mvn -pl app spring-boot:run
    ```

---

## Backend Startup & Configuration

### Out-of-the-Box Dev Profile Defaults
The development environment activates `spring.profiles.active=dev` by default, offering zero-configuration local setup:
- **Embedded H2 Database**: Uses a local file database at `${user.dir}/db/dati` with automatic schema updates (`ddl-auto: update`). **No MySQL installation required.**
- **H2 Web Console**: Enabled by default at `http://localhost:8085/h2-console/dati` (JDBC URL: `jdbc:h2:file:./db/dati`, Username: `sa`, Password: leave blank; click "Connect" directly).
- **API Documentation (Swagger)**: Available at `http://localhost:8085/swagger-ui.html`.
- **Security & Admin**: Preconfigured local `JWT_SECRET`; default admin identifier is `admin` (`ADMIN_USERS=admin`).
- **MCP Endpoint**: `dati.mcp.endpoint-base-url` automatically resolves to `http://localhost:8085`.

### Key Configurations to Review (`application-dev.yaml`)
Before starting the backend, verify `backend/app/src/main/resources/application-dev.yaml`:

1. **Elasticsearch Connection & Credentials**
   ```yaml
   spring:
     elasticsearch:
       uris: ${SPRING_ELASTICSEARCH_URIS:http://localhost:9200}
       username: ${SPRING_ELASTICSEARCH_USERNAME:elastic}
       password: ${SPRING_ELASTICSEARCH_PASSWORD:}
   ```
   - **Default Behavior**: Password is empty by default, pairing with the no-auth Docker command above. **No configuration changes needed to start.**

2. **Switching to External Database (Optional)**
   - To use MySQL instead of H2, adjust `spring.datasource.*` in `application-dev.yaml`.

3. **Elasticsearch Wire Logging (Optional)**
   - To inspect raw Elasticsearch DSL queries and HTTP wire traffic, uncomment:
     ```yaml
     logging:
       level:
         org.apache.http.wire: DEBUG
     ```

### Starting the Backend
- **Via IDE (Recommended)**: Run the `com.dati.DatIApplication` main class.
- **Via CLI**:
  ```bash
  cd backend
  mvn spring-boot:run
  ```
Check `http://localhost:8085/actuator/health`. When `status` reports `UP` (with both `elasticsearch` and `db` components UP), the backend is ready.

---

## Frontend & Help Center Startup

The frontend is built with Vue 3 + TypeScript + Vite and embeds a VitePress-powered user help center:

```bash
cd frontend
pnpm install

# Start the management console (port 5173)
pnpm dev

# (Optional) Start the Help Center documentation server (port 5174)
pnpm docs:dev
```

- Management console runs at `http://localhost:5173`.
- **API Proxy**: Automatically forwards `/v1` and `/api` requests to the local backend at `http://localhost:8085`.
- **Help Center Integration**: The "Help Docs" link in the top navigation points to `/docs` (reverse-proxied to port `5174`). Keep `pnpm docs:dev` running in the background to edit or preview documentation with hot reloading.
- **Production Build**: Use `pnpm build:all` (compiles VitePress docs into `public/docs` first, then builds the frontend SPA).

---

## Key Development Notes

1. **Initial Admin Registration**
   - The platform has **no pre-seeded accounts**.
   - On your first visit, register with the username `admin`. The system recognizes this username (matching `ADMIN_USERS=admin`) and automatically grants super administrator privileges.
2. **JSON Naming Convention**
   - The dev environment uses `SNAKE_CASE` JSON naming globally. Write `camelCase` in Java code and DTO fields; serialization automatically converts them to `snake_case`.
