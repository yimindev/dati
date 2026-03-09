# Project Guidelines

These guidelines capture project-specific details for building, testing, and developing in this repository. They are intended for advanced contributors and focus on what is special about this codebase.

- Repository root: data-conn-ai
- Modules:
  - backend: Spring Boot 3.5.x (Java 21), Maven, JPA/JDBC, Flyway, H2/MySQL/PostgreSQL drivers
  - frontend: Vue 3 + Vite + TypeScript, TailwindCSS, Element Plus
- Toolchain baseline:
  - Java 21
  - Maven 3.9+
  - Node.js 20+ and pnpm 10+


## 1) Build and configuration

Backend (Spring Boot)
- Build the module only: cd backend && mvn -B -DskipTests package
- Run in dev profile (default):
  - mvn spring-boot:run
  - or: java -jar target/backend-*.jar (the default active profile is dev)
- Profiles and ports:
  - application.yaml sets server.port=8085 and spring.profiles.active=dev
  - application-dev.yaml configures H2 and Snake Case JSON
- Database (dev):
  - H2 file-based store at ./db/dataconnai relative to the project root
  - JDBC URL in dev: jdbc:h2:file:./db/dataconnai;QUERY_TIMEOUT=30
  - H2 web console enabled at /h2-console/semantic when the app is running
- JPA:
  - spring.jpa.hibernate.ddl-auto=update for dev (be cautious when changing schema)
- Flyway:
  - Flyway dependencies are present; add migrations under backend/src/main/resources/db/migration (V1__*.sql etc.) if/when needed

Frontend (Vite/Vue 3 + TS)
- Install and type-check/build:
  - cd frontend && npm install
  - npm run build (runs vue-tsc -b then vite build)
  - npm run dev for local dev server
- Preview a production build: npm run preview


## 2) Testing: how to run and how to add tests

Backend (JUnit 5 via Maven Surefire)
- Run all tests for backend: cd backend && mvn test
- Run a specific test class: mvn -Dtest=com.dati.DatiApplicationTests test
- Run a specific test method: mvn -Dtest=com.dati.DatiApplicationTests#contextLoads test
- Run tests skipping integration tests (if categorized later): use Maven profiles or surefire/failsafe includes; currently only unit tests exist
- IDE runs: any modern IDE will detect JUnit 5 tests; contextLoads smoke test already exists in backend/src/test/java/com/dataconnai/BackendApplicationTests.java

Adding a new unit test (backend)
- Location: backend/src/test/java/<your package>/YourTest.java
- Minimal example skeleton:
  - package com.dati;
  - import org.junit.jupiter.api.Test;
  - import static org.junit.jupiter.api.Assertions.*;
  - class YourTest { @Test void works() { assertTrue(true); } }
- After adding, run: mvn -Dtest=com.dati.YourTest test

Validation performed
- A simple JUnit 5 test was executed successfully locally (see com.dati.DatiApplicationTests::contextLoads). The commands above were validated.

Frontend testing
- There is currently no test runner configured in the frontend package.json. If you need unit tests, add Vitest and configure scripts, for example:
  - npm i -D vitest @vitest/ui jsdom
  - add to package.json scripts: "test": "vitest", "test:ui": "vitest --ui"
  - create tests under frontend/src with *.test.ts and run npm run test
- Type checking is enforced in the build via vue-tsc (npm run build runs vue-tsc -b). You can run type-check only with: npx vue-tsc --noEmit


## 3) Additional development information

Backend specifics
- Code style: Java 21, Lombok is used (ensure annotation processing is enabled in your IDE). Avoid adding boilerplate that Lombok already provides.
- Exception handling: com.dati.base.exception.DciException exists; prefer throwing domain-specific exceptions for API layers.
- Database utilities: com.dati.db.* provides DB client abstractions (DbClientFactory, JdbcUtils, etc.). For connection testing or metadata, use these utilities rather than bespoke JDBC code.
- JSON naming: spring.jackson.property-naming-strategy=SNAKE_CASE in dev; align DTO fields and frontend expectations accordingly.
- Ports and CORS: server runs on 8085 by default; if adding controllers that need cross-origin, define appropriate CORS config.

Frontend specifics
- Stack: Vue 3 (script setup + TS), Vite, Element Plus UI, Tailwind v4 plugin.
- Module auto-imports: unplugin-auto-import and unplugin-vue-components are present; keep component names PascalCase and colocate components under src/components.
- Lint/format: ESLint and Prettier dependencies exist but there are no scripts configured; consider adding lint scripts if you plan to enforce linting in CI:
  - npm i -D eslint prettier && npx eslint --init (or use @vue/eslint-config-* presets already present)

Working with profiles and local data
- Dev profile is the default; if you need a clean H2 dev DB, delete ./db/dataconnai.* files and restart (data will be recreated by JPA update strategy).
- For MySQL/PostgreSQL dev, switch spring.datasource.* in a new profile (e.g., application-mysql.yaml) and run with -Dspring-boot.run.profiles=mysql.

CI/CD considerations
- Use mvn -B -DskipTests package in CI for build-only steps; run mvn -B test as a separate stage.
- Frontend can be built with npm ci && npm run build. The output dist/ can be published by your chosen static host.
