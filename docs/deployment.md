# Server Deployment Guide (Docker Compose)

[English](deployment.md) | [简体中文](deployment_zh.md)

This document describes the single-node containerized deployment and operational maintenance of DatI.

---

## Architecture Overview

DatI runs as two container services via Docker Compose, configured by default for a 2-core 4GB RAM host (peak usage ~2.5GB):

1. **`dati-app` (Application Container)**
   - **All-in-One Image**: Built locally via multi-stage `Dockerfile`. Compiles the frontend (Vue 3 console) and Help Center (VitePress) static assets directly into the backend Spring Boot Fat JAR. Runs as a single container listening on port `8085`.
   - **Data Persistence**: Uses an embedded H2 database by default, persisted in the `dati_data` volume (can be pointed to external MySQL / PostgreSQL in `.env`).
   - **Resource Budget**: Memory limit capped at 1.5GB (JVM heap auto-allocates ~75%).

2. **`elasticsearch` (Semantic Search Container)**
   - **Image & Plugin**: Uses `davyinsa/elasticsearch-ik:8.18.7` (pre-installed IK Chinese analyzer), protocol-aligned with the application's ES 8 Java Client, listening on port `9200`.
   - **Data Persistence**: Semantic search indices persisted in the `es_data` volume.
   - **Resource Budget**: Memory limit capped at 1GB, JVM heap pinned to `-Xms512m -Xmx512m`.

---

## Quick Start

Enter the project root directory:
```bash
cd dati
```

### 1. Configure Environment Variables
Copy the template file:
```bash
cp .env.example .env
```
Edit `.env` and fill in the required secrets:
- `JWT_SECRET`: Secret key for signing tokens (random string, minimum 32 characters).
- `SPRING_ELASTICSEARCH_PASSWORD`: Password for Elasticsearch authentication.

### 2. Build and Start Services
```bash
docker compose up -d --build
```

### 3. Verification & Initial Setup
- **Health Check**: Visit `http://<host>:8085/actuator/health`. A response of `{"status":"UP"}` indicates both application and search engine are healthy.
- **Admin Registration**: Open `http://<host>:8085` in your browser. The platform has **no pre-seeded accounts**; register with username `admin` (matching `ADMIN_USERS` in `.env`) to automatically receive super administrator privileges.

---

## Configuration & Caveats

1. **Elasticsearch Password Initialization (Volume Persistence)**
   - The ES password is set only during the initial creation of the `es_data` volume.
   - If you modify `SPRING_ELASTICSEARCH_PASSWORD` in `.env` later, you must recreate the ES volume:
     ```bash
     docker compose down
     docker volume rm dati_es_data
     docker compose up -d
     ```
     (Semantic indices can be rebuilt by re-syncing metadata in the platform; business data in H2/MySQL remains intact).

2. **External Database Configuration**
   - Uses embedded H2 by default (persisted in `dati_data` volume).
   - To connect external MySQL / RDS, configure `.env`:
     ```env
     SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/dati?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
     SPRING_DATASOURCE_USERNAME=root
     SPRING_DATASOURCE_PASSWORD=your_password
     ```
     The application updates the schema automatically on startup (`ddl-auto=update`). When using cloud RDS, whitelist the host outbound IP.

3. **Public Access Control**
   - The user registration endpoint (`/v1/auth/register`) is public by design. If deploying to the public internet and you wish to restrict signups, configure IP whitelisting in your reverse proxy or firewall.

---

## Common Operations

| Operation | Command | Description |
|-----------|---------|-------------|
| Upgrade | `git pull && docker compose up -d --build` | Rebuild image locally and restart containers; volumes preserved |
| View App Logs | `docker compose logs -f dati-app` | Tail application and API access logs |
| View ES Logs | `docker compose logs -f elasticsearch` | Tail Elasticsearch runtime and indexing logs |
| Restart Services | `docker compose restart` | Restart containers without modifying data |
| Complete Reset | `docker compose down -v` | Destroy all containers and volumes (purges all data, use with caution) |

---

## Troubleshooting

| Symptom | Resolution |
|---------|------------|
| `must be set in .env` error on startup | Ensure `.env` exists and that required variables (`JWT_SECRET`, `SPRING_ELASTICSEARCH_PASSWORD`) are populated. |
| App log reports `ES 401 Unauthorized` | Check if ES password was modified after volume creation. Recreate `dati_es_data` volume as described above. |
| `/actuator/health` reports `DOWN` | Run `docker compose logs dati-app` to check whether the `db` or `elasticsearch` component failed. |
| Container exits due to OOM | Ensure host has at least 2.5GB available memory. Configure 1–2GB swap on smaller instances. |
