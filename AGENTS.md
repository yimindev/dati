# AGENTS.md

This file guides agentic coding assistants working on the DatI repository.

## Project Overview

**DatI (Data Intelligence)** is a platform that provides **unified data access capabilities** for large language models (LLMs).
- **Core Features**: Quickly connect to various data sources (databases, APIs, files) and automatically generate interfaces complying with the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) standard.
- **Use Cases**: Building NL2SQL applications, data analysis assistants, and rapid deployment of data query MCP services.

## General Principles

1. **Code should be simple and clean, never over-complicate things.**
2. **Write a detailed test suite as you add more features.** The test must be re-executed at every major change.
3. **Stage new files to git before running tests.** Uncommitted changes make it harder to verify what was added or modified.
4. **English in code, Chinese in docs.** Log messages, test `@DisplayName`, console output, and comments use English; Chinese is reserved for user-facing text (i18n) and PRD docs.

## Commands

### Backend (Java/Spring Boot)
```bash
cd backend
mvn test                                  # Run all tests
mvn -Dtest=ClassName#MethodName test      # Run specific test
mvn -B -DskipTests package                # Build without tests
mvn spring-boot:run                       # Run (port 8085)
```

### Frontend (Vue 3 + TypeScript)
```bash
cd frontend
pnpm install                              # Install deps
pnpm build                                # Type check + Build
pnpm dev                                  # Dev server
```

## Testing Requirements (CRITICAL)

- **Backend**: New features **MUST** be developed using **TDD**. Write a failing test first, then implement the minimal code to pass it.
- **Frontend**: Unit tests use Vitest.
- Always verify changes by running relevant tests before submission.

## Backend at a Glance

**Package structure (DDD):**
```
com.dati.<module>/
├── domain/{ model, service }          # Domain entities + business logic
├── repository/{ dao, po, mapper }     # Persistence + PO↔Model conversion
└── server/{ controller, pojo, assembler }  # REST + Model↔VO conversion
```

- **Mapper**: static methods, PO↔Model, handles encryption. **Assembler**: `@Component extends BaseAssembler`, Model↔VO, fills user info.
- **Naming**: `PascalCase` classes, `camelCase` methods, `lowercase` packages.
- **Injection**: Constructor injection only (`private final`).
- **Lombok**: POs use `@Getter/@Setter`, optional `@FieldNameConstants` (PO-only), `@Slf4j` as needed.
- **Response**: `IdResponse` for mutations, `PageResponse<T>` for paginated lists, raw `List<T>` for metadata queries.
- **Exception**: Use `DatiException` with `ErrorCode` enum (prefixes: `CM` common, `DS` datasource, `SM` semantic).
- **JSON**: Dev profile uses `SNAKE_CASE`.

→ Full details: [.agents/rules/backend.md](.agents/rules/backend.md)

## Frontend at a Glance

```
src/
├── pages/          # File-based routing (unplugin-vue-router)
├── components/     # Auto-imported, grouped by <feature>
├── api/            # http.ts (axios) + types.ts + <feature>.ts
├── composables/    # Reusable composition functions
├── stores/         # Pinia (setup syntax)
├── locales/        # i18n (zh.ts, en.ts)
└── styles/         # TailwindCSS 4 + Element Plus
```

- **SFC**: `<script setup lang="ts">`, components use `PascalCase`.
- **Types**: `snake_case` for API JSON, `camelCase` for local variables.
- **API**: All calls via `api/http.ts`, interceptor returns `resp.data` directly.
- **State**: Pinia with setup syntax (see `stores/system.ts`).
- **Styling**: TailwindCSS 4 + Element Plus, prefer atomic utility classes.
- **I18n**: `t('key')` for all user-facing text, keys nested by feature.

→ Full details: [.agents/rules/frontend.md](.agents/rules/frontend.md)

## Design System

The project follows a **Flat Design + Enterprise Minimal** style with Element Plus as the UI framework.

**Key rules (always apply):**

| Rule | Standard |
|------|----------|
| **Colors** | Use `var(--ep-color-*)` semantic variables everywhere. Never raw hex in component templates. |
| **Typography** | Page title `24px/650`, section title `16px/650`, body `14px`, meta `13px`, label `12px`. |
| **Spacing** | Tailwind 4px base unit: `gap-1`=`4px`, `gap-2`=`8px`, `gap-4`=`16px`. |
| **Page layout** | List pages: `p-5 md:p-6` → toolbar → `DataTableShell` → dialog. Detail pages with breadcrumb + tabs or side nav. |
| **Buttons** | Primary CTA: `el-button type="primary"`. Table actions: `type="primary" link`. Danger: `type="danger" link`. |
| **Icons** | SVG only via Element Plus icons (`@element-plus/icons-vue`) or Iconify. **Never emoji as structural icons.** |
| **Loading** | Tables: `v-loading="loading"`. Card grids: `el-skeleton`. Buttons: `:loading="loading"`. |
| **Empty states** | Use `el-empty` with contextual `description` via i18n `t('xxx.emptyList')`. |
| **Dark mode** | `useDark()` from `@vueuse/core`. Element Plus CSS vars auto-switch. Test both modes. |
| **Accessibility** | Icon-only buttons need `aria-label` or `el-tooltip`. Deletions need `ElMessageBox.confirm`. Add `cursor-pointer` to all clickables. |
| **Responsive** | `p-5 md:p-6` for page padding, flex-wrap + flex-col on mobile breakpoints (768px). |

**Page templates available for copy-paste:**
- List page (table) — MCP Services / Data Sources pattern
- List page (card grid) — Subjects pattern
- Detail page (tabs inside card) — Subject detail pattern
- Detail page (side nav) — MCP Service detail pattern

→ Full design system details (colors, typography scale, spacing, component patterns, anti-patterns): [.agents/rules/design-system.md](.agents/rules/design-system.md)
