# AGENTS.md

This file guides agentic coding assistants working on the DatI repository.

## Build, Lint, Test Commands

### Backend (Java/Spring Boot)
```bash
cd backend

# Run all tests
mvn test

# Run a single test class
mvn -Dtest=com.dati.DatiApplicationTests test

# Run a single test method
mvn -Dtest=com.dati.DatiApplicationTests#contextLoads test

# Build without tests
mvn -B -DskipTests package

# Run application (dev profile, port 8085)
mvn spring-boot:run
```

### Frontend (Vue 3 + TypeScript)
```bash
cd frontend

# Install dependencies (first time)
pnpm install

# Type check (runs before build)
pnpm exec vue-tsc -b

# Full build (type check + vite build)
pnpm build

# Dev server
pnpm dev

# Preview production build
pnpm preview
```

Note: No dedicated lint npm script - ESLint available in devDependencies. Typecheck runs as part of `pnpm build`.

## Backend Code Style

### Package Structure (DDD-inspired)
- `domain/model/`: Domain entities (e.g., `DataSource`, `TableInfo`)
- `repository/dao/`: JPA DAO interfaces extending `JpaRepository`
- `repository/po/`: Persistence objects (PO) with JPA annotations
- `repository/mapper/`: PO ↔ Model mappers (e.g., `DSMapper`)
- `repository/converter/`: Custom attribute converters
- `server/controller/`: REST controllers
- `server/assembler/`: Model ↔ VO assemblers
- `server/pojo/`: Request/Response DTOs (VO)
- `domain/service/`: Business logic services

### Naming Conventions
- Classes: `PascalCase` (e.g., `DataSourceService`, `DSAssembler`)
- Methods: `camelCase` (e.g., `listDataSources`, `testConnection`)
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase` (e.g., `com.dati.datasource.domain`)

### Imports
- Order: standard, third-party, project (blank lines between groups)
- No wildcard imports except static constants
- Lombok: `@Data`, `@Slf4j`, `@EqualsAndHashCode(callSuper = true)` common

### Formatting & Types
- Use Lombok annotations instead of boilerplate getters/setters
- Services: Constructor injection only (`private final` fields)
- Controllers: `@Slf4j`, `@RestController`, `@RequestMapping("/v1/...")`
- DTOs: Return `IdResponse` for create/update, `PageResponse<T>` for lists

### Error Handling
- Wrap checked exceptions (e.g., `SQLException`) in `DciException` with user-friendly message
- Log errors: `log.error("Failed to ... for datasource {}", id, e)`
- Return `boolean` for test operations, throw `DciException` for failures

### JPA & Database
- Dev: `ddl-auto=update` (H2 file db at `./db/dataconnai`)
- Production: Use Flyway migrations in `db/migration/`
- Base class: `BaseResourcePO` provides `id`, `createdAt`, `updatedAt` fields

## Frontend Code Style

### File Structure
- `pages/`: Route components (auto-imported by unplugin-vue-router)
- `components/`: Reusable components (auto-registered by unplugin-vue-components)
- `api/`: API functions and types
- `composables/`: Composable functions
- `locales/`: i18n translations (en.ts, zh.ts)

### Component Conventions
- Use `<script setup lang="ts">`
- Components: `PascalCase` (e.g., `DatasourceForm.vue`)
- Props/Emits: Define interfaces explicitly
- Template refs: Use `ref<T | null>()`

### TypeScript & Types
- API types: Snake_case to match backend JSON (e.g., `jdbc_url`, `created_at`)
- Local types: camelCase
- Use generic types from `~/api/types.ts`: `IdResponse`, `PageResponse<T>`, `BaseResourceVO`
- Avoid `any` - use `unknown` for dynamic data, `Record<string, unknown>` for objects

### Imports & Auto-imports
- Vue APIs (`ref`, `computed`, `onMounted`) auto-imported
- Components auto-registered
- Element Plus icons: `import { Plus, Search } from "@element-plus/icons-vue"`
- API functions: Import explicitly from `~/api/...`

### Styling
- TailwindCSS 4 utility classes
- Element Plus components for UI
- i18n: `const { t } = useI18n()`, use `t('key')` in templates

### API Calls
- Use typed functions from `api/` with AbortSignal support
- Error handling: `try/catch` with `ElMessage.error(t('...'))`
- Loading states: `ref(false)` with `finally` block
- Pagination: Manage `page`, `pageSize`, `total` in parent components

## Architecture Notes

### Backend Layers
Controller → Service → DAO/Repository → Domain Model → JPA/Database
Assemblers handle PO/VO conversions, Mappers handle Model/PO conversions

### Frontend Data Flow
Component → API function → http client → Backend API → Response → Component state

### Common Patterns
- Base classes: `BaseResource`, `BaseResourcePO`, `BaseResourceVO`, `BaseAssembler`
- Pagination: `PageReq` → `PageRequest` → `Page<T>` → `PageResponse<T>`
- User context: `RequestContext.getUser()` for current user ID
