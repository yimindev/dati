# DatI Design System (Master)

> **Generated**: 2026-06-09
> **Project**: DatI — Data Intelligence Platform
> **Stack**: Vue 3 + TypeScript + Element Plus + TailwindCSS 4
> **Style**: Flat Design + Enterprise Minimal

---

## Table of Contents

1. [Brand & Identity](#1-brand--identity)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Spacing & Layout](#4-spacing--layout)
5. [Component Patterns](#5-component-patterns)
6. [Animation & Interaction](#6-animation--interaction)
7. [Iconography](#7-iconography)
8. [Dark Mode](#8-dark-mode)
9. [Responsive Breakpoints](#9-responsive-breakpoints)
10. [Accessibility](#10-accessibility)
11. [Patterns & Templates](#11-patterns--templates)
12. [Contributing & Code Conventions](#12-contributing--code-conventions)

---

## 1. Brand & Identity

### 1.1 Brand Name
**DatI** (Data Intelligence)

### 1.2 Logo
Located at `/public/dati.svg` — used in the sidebar header, login/auth pages.

### 1.3 Product Category
- B2B data platform / SaaS
- Target users: Developers, Data Engineers, AI/ML Engineers
- Personality: **Professional, Clean, Technical, Trustworthy**

### 1.4 Tone
| Dimension | Value |
|-----------|-------|
| Voice | Technical but approachable |
| Adjective | Clean, modern, data-forward |
| Formality | Professional, not overly casual |
| Complexity | Complex concepts presented simply |

---

## 2. Color System

### 2.1 Semantic Color Tokens

The project uses **Element Plus CSS variables** (`--ep-*`) exclusively for semantic colors in components. **No raw hex values in component templates** (exceptions: neutral grays in scoped styles for specific text colors).

#### Primary Color
| Token | Current Value | Usage |
|-------|--------------|-------|
| `--ep-color-primary` | `green` (light) / `#589ef8` (dark) | Primary buttons, links, active states |
| `--ep-color-primary-light-*` | Calculated tints | Backgrounds, hover states |
| `--ep-color-primary-dark-*` | Calculated shades | Pressed states |

**Note:** The primary color is set in `element/index.scss` as `green` for light mode and overridden in `dark.scss` as `#589ef8` for dark mode. Consider aligning both modes to a consistent brand blue for a more professional enterprise feel.

#### Semantic Colors
| Token | Light Value | Usage |
|-------|------------|-------|
| `--ep-color-primary` | `green` | Primary actions, links |
| `--ep-color-success` | `#21ba45` | Success states, published status |
| `--ep-color-warning` | `#f2711c` | Warnings, caution states |
| `--ep-color-danger` | `#db2828` | Destructive actions, errors |
| `--ep-color-info` | `#42b8dd` | Informational, neutral status |
| `--ep-color-error` | `#db2828` | Error messages, validation |

#### Surface & Text Colors
| Token | Light Value | Role |
|-------|-------------|------|
| `--ep-bg-color` | `#ffffff` | Page / card background |
| `--ep-fill-color-light` | `#f5f7fa` | Content area background |
| `--ep-fill-color-lighter` | `#fafafa` | Subtle hover / meta item bg |
| `--ep-bg-color` | `#ffffff` | Dialog, card, panel bg |
| `--ep-text-color-primary` | `#303133` | Primary headings, names |
| `--ep-text-color-regular` | `#606266` | Body text |
| `--ep-text-color-secondary` | `#909399` | Secondary text, metadata |
| `--ep-text-color-placeholder` | `#c0c4cc` | Placeholder text |
| `--ep-border-color-lighter` | `#ebeef5` | Borders (cards, tables, panels) |
| `--ep-border-color` | `#dcdfe6` | Form controls, inputs |
| `--ep-menu-hover-bg-color` | Calculated | Side menu hover |

### 2.2 Background Layering

```
Layer 0: --ep-fill-color-light (#f5f7fa)    → Page background (App.vue main container)
Layer 1: --ep-bg-color (#ffffff)              → Card / panel / table content
Layer 2: --ep-fill-color-lighter (#fafafa)    → Subdued containers, meta items
Layer 3: --ep-fill-color (#f0f2f5)            → Tag backgrounds, input disabled
```

### 2.3 Color Usage Rules

| Rule | Standard | Avoid |
|------|----------|-------|
| Semantic tokens | Use `var(--ep-color-*)` everywhere | Raw hex colors in component styles |
| Brand primary | Single primary green across all pages | Different primary per page |
| Danger actions | Always `--ep-color-danger` (`#db2828`) | Non-red colors for destructive actions |
| Status badges | Use `el-tag` with appropriate `type` | Custom colored badges |
| Text hierarchy | Use `--ep-text-color-*` tokens | Arbitrary gray values |

### 2.4 Recommended Brand Refresh (Future)

If moving to a consistent brand identity:

| Token | Recommended | Current |
|-------|-------------|---------|
| Primary (light) | `#2563eb` (blue-600) | `green` |
| Primary (dark) | `#589ef8` (already set) | `#589ef8` |
| Accent | `#0891b2` (cyan-600) | — |
| Success | `#16a34a` (green-600) | `#21ba45` |
| Background | `#f8fafc` (slate-50) | `#f5f7fa` |

---

## 3. Typography

### 3.1 Font Stack

```css
--font-family-base: Inter, system-ui, Avenir, 'Helvetica Neue', Helvetica,
  'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
```

Defined in `styles/index.scss` on `body`.

### 3.2 Type Scale

The project does not use a global type scale. Instead, it uses **ad-hoc sizing** with Tailwind classes and Element Plus defaults. Define this scale moving forward:

| Level | Size | Weight | Line-Height | Usage |
|-------|------|--------|-------------|-------|
| h1 | `20px` | `650` / `font-semibold` | `28px` | Page title (heading) |
| h2 | `16px` | `650` / `font-semibold` | `24px` | Section title in panels |
| h3 | `15px` | `600` | `22px` | Card titles, sub-sections |
| Body | `14px` | `400` | `22px` | Default text |
| Small / Meta | `13px` | `400` | `20px` | Secondary info, descriptions |
| Label | `12px` | `500` | `18px` | Tags, captions |
| Micro | `11px` | `500` | `16px` | Badge counts, tiny meta |

### 3.3 Typography Conventions

| Rule | Standard | Avoid |
|------|----------|-------|
| Page titles | `h1` tag, `font-size: 24px`, `font-weight: 650` | Generic div for page heading |
| Section titles | `h2` inside panels, `font-size: 16px`, `font-weight: 650` | Inconsistent sizing |
| ID / code values | `font-mono` class, `#589ef8` (dark) or slate-500 | Body font for technical IDs |
| Body text | `font-size: 13-14px` | Text under `12px` |
| Truncation | `truncate` / `show-overflow-tooltip` on tables | Content overflow without handling |

### 3.4 Font Weight Mapping

| Weight | Token | Usage |
|--------|-------|-------|
| `400` | Regular | Body, descriptions |
| `500` | Medium | Labels, menu items |
| `600` | Semibold | Sub-headings, emphasized text |
| `650` | — | H1, H2 page/section titles |
| `700` | Bold | Brand name, strong emphasis |

---

## 4. Spacing & Layout

### 4.1 Spacing System

Use Tailwind's **4px base unit** (consistent with Element Plus spacing):

| Class | Value | Usage |
|-------|-------|-------|
| `gap-1` | `4px` | Tight icon-text gaps |
| `gap-1.5` | `6px` | Small control spacing |
| `gap-2` | `8px` | Button groups, tag spacing |
| `gap-3` | `12px` | Form items, action groups |
| `gap-4` | `16px` | Section spacing, card grid |
| `gap-6` | `24px` | Between related sections |
| `gap-8` | `32px` | Major section separation |

### 4.2 Page Layout

```
┌─────────────────────────────────────────────────────┐
│ BaseHeader (horizontal menu, fixed top)              │
├──────────────┬──────────────────────────────────────┤
│              │                                      │
│  BaseSide    │  Main Content Area                   │
│  (collapsible) │  ├── p-5 md:p-6 (page padding)     │
│              │  │  └── space-y-4 (section spacing)  │
│  w-16 / w-50 │  │                                   │
│              │  └── bg-[var(--ep-fill-color-light)] │
│              │                                      │
└──────────────┴──────────────────────────────────────┘
```

**Key layout rules:**
- `.main-container` = `height: calc(100vh - var(--ep-menu-item-height) - 4px)`
- Sidebar: collapsible `w-16` / `w-50` (64px / 200px)
- Content area: `flex-1 min-w-0 bg-[var(--ep-fill-color-light)]`
- List/sub-pages use `.list-page` class (defined globally in `styles/index.scss`): flex column, `gap: 16px`, `padding: 24px`, responsive → `16px` at 768px
- Section spacing: `gap-4` via flex column layout

### 4.3 Global Shared Classes

These CSS classes are defined in `frontend/src/styles/index.scss` and shared across all list/sub-pages:

| Class | Usage |
|-------|-------|
| `.list-page` | Page wrapper, flex column, 24px padding, 16px gap |
| `.page-heading` | Flex row, h1 + optional subtitle left, `.heading-actions` right |
| `.page-heading h1` | Page title: 20px, weight 650 |
| `.page-heading p` | Subtitle: 13px, secondary color |
| `.heading-actions` | Action buttons container, flex row, 10px gap |
| `.toolbar` | Search/filter toolbar: 1px border, 8px radius, 14px padding |
| `.toolbar-fields` | Inner flex row, 10px gap |
| `.toolbar-search` | Search input: `min(420px, 100%)` width |
| `.detail-header` | Breadcrumb + actions row (sub-pages) |

Responsive at 768px: `.page-heading`, `.heading-actions`, `.detail-header`, `.detail-actions`, `.toolbar-fields` stack vertically; `.toolbar-search` becomes 100% width.

### 4.3 Card & Panel Patterns

| Component | Border | Shadow | Border-Radius | Padding |
|-----------|--------|--------|---------------|---------|
| `el-card` | `1px solid var(--ep-border-color-lighter)` | `hover` / `never` / `always` | `8px` | `--el-card-padding` |
| `.data-table-shell` | `1px solid var(--ep-border-color-lighter)` | None | `8px` | — |
| `.panel` (detail page) | `1px solid var(--ep-border-color-lighter)` | None | `8px` | `18px` |
| `.toolbar` (global class) | `1px solid var(--ep-border-color-lighter)` | None | `8px` | `14px` |

### 4.4 Layout Anti-patterns

| ❌ Avoid | ✅ Do Instead |
|----------|---------------|
| Fixed pixel container widths | Use responsive `max-w-*` / `w-full` |
| Nested horizontal scroll | Use `min-w-0 overflow-hidden` on flex children |
| Content behind sidebar/header | Use `calc(100vh - header)` height |
| Inconsistent page padding | Always use `p-5 md:p-6` on page root |
| Cramped mobile layout | Use `flex-col` on mobile breakpoints |

---

## 5. Component Patterns

### 5.1 Pages (List Pages)

Every list page follows this template (classes are globally shared from `styles/index.scss`):

```html
<div class="list-page">
  <!-- 1. Page Heading -->
  <div class="page-heading">...</div>

  <!-- 2. Toolbar -->
  <div class="toolbar">
    <div class="toolbar-fields">
      <el-input class="toolbar-search" ... />
    </div>
  </div>

  <!-- 3. Data Table -->
  <DataTableShell ...>
    <XxxTable :data="list" />
  </DataTableShell>

  <!-- 4. Dialog -->
  <XxxDialog v-model="dialogVisible" ... />
</div>
```

**Concrete examples:**
- `mcp-services/index.vue` — uses `.page-heading` + `.toolbar` + `DataTableShell`
- `datasources/index.vue` — uses inline toolbar + `DataTableShell`
- `subjects/index.vue` — card grid with `SubjectCard` + inline toolbar

**Two toolbar patterns:**

| Pattern | Used In | Structure |
|---------|---------|-----------|
| **Compact** | `datasources/`, `subjects/`, `tables/` | Inline flex row, search + button |
| **Structured** | `mcp-services/` | `.page-heading` (title+subtitle+actions) + `.toolbar` (filters) |

### 5.2 DataTableShell

A reusable wrapper providing consistent shell styling + pagination.

```
┌──────────────────────────────────────────┐
│  DataTableShell                          │
│  ┌────────────────────────────────────┐  │
│  │  <slot> (el-table)                │  │
│  │  ...                               │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │ "共 N 条"         [pagination]     │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

**Rules:**
- Always use `DataTableShell` for table-backed list pages
- Table footer: 14px padding, border-top
- Pagination: `sizes, prev, pager, next` layout
- Default page sizes: `[10, 20, 50, 100]`

### 5.3 Dialogs

All CRUD dialogs follow a consistent structure:

**Container:** `el-dialog` with `width="35%"` or `width="600px"` / `width="780px"`
**Props:** `v-model`, `:close-on-click-modal="false"`
**Content:** Form inside dialog body
**Footer:** `Cancel` + `Save/Create` buttons

```html
<el-dialog v-model="visible" :title="..." width="35%" :close-on-click-modal="false">
  <!-- Form body -->
  <template #footer>
    <div class="dialog-footer">
      <el-button @click="cancel">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ isEdit ? t('common.update') : t('common.create') }}
      </el-button>
    </div>
  </template>
</el-dialog>
```

**Dialog widths (3 tiers only):**
| Tier | Width | When | Examples |
|------|-------|------|----------|
| Small | `35%` | Simple forms + confirmations | SubjectDialog, DatasourceDialog, sync/confirm dialogs |
| Medium | `600px` | Standard CRUD forms | McpServiceDialog, TemplatePreviewDialog, table metadata, TermManager |
| Large | `780px` | Complex forms (tables, transfers, security config) | PromptDialog, CustomToolDialog, DataScopeTab, values dialog, relation dialog |

### 5.4 Form Patterns

**Label position:**
- Simple dialogs: `label-width="120px"` (horizontal)
- Detail page edit: `label-position="top"` (stacked)
- Filter forms: inline layout

**Form rules:**
```ts
const rules: FormRules = {
  name: [
    { required: true, message: t('common.required', { name: t('common.name') }), trigger: 'blur' },
    { min: 1, max: 100, message: t('common.nameLengthError'), trigger: 'blur' },
  ],
}
```

**Alias input pattern** (reused across subjects, table metadata, terms):
```html
<div class="flex gap-2 flex-wrap">
  <el-tag v-for="alias in aliases" :key="alias" closable @close="removeAlias(alias)">
    {{ alias }}
  </el-tag>
  <el-input v-if="inputVisible" ref="inputRef" v-model="newAlias" class="w-20" size="small"
    @keyup.enter="confirmAlias" @blur="confirmAlias" />
  <el-button v-else size="small" @click="showInput">+ {{ t('common.aliases') }}</el-button>
</div>
```

### 5.5 Table Column Conventions

| Column | Config | Notes |
|--------|--------|-------|
| ID | `min-width="120" show-overflow-tooltip` | Use monospace for technical IDs |
| Name | `min-width="140-220"` | May be a clickable link |
| Description | `min-width="160-240" show-overflow-tooltip` | Or shorter |
| Status | `min-width="120"`, use `el-tag` with status type | Color + label |
| Updated At | `min-width="160"`, use `formatDateTime` | Via composable |
| Actions | `width="180-220" fixed="right"` | Link buttons |

**Action button patterns:**
```html
<!-- Inline visible actions (≤4) -->
<el-button type="primary" link @click="edit">Edit</el-button>
<el-button type="danger" link @click="delete">Delete</el-button>

<!-- Overflow actions (>4), use dropdown -->
<el-dropdown trigger="click">
  <el-button link :icon="MoreFilled" />
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item @click="...">Action</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

### 5.6 Button Hierarchy

| Level | Type | Size | Usage |
|-------|------|------|-------|
| Primary CTA | `el-button type="primary"` | `default` | Create, Save, Submit |
| Secondary | `el-button type="primary" plain` | `default` | Search, Filter |
| Tertiary | `el-button` (default) | `default` / `small` | Cancel, Back |
| Text/Link | `el-button type="primary" link` | `default` | Table actions, inline |
| Danger | `el-button type="danger" link` or `plain` | `default` | Delete, Remove |
| Icon only | `el-button :icon="Icon" link` | `default` | Copy, More, Toggle |

### 5.7 Empty States

Use `el-empty` with contextual `description`:
```html
<el-empty :description="t('xxx.emptyList')" />
```

### 5.8 Loading States

- Tables: `v-loading="loading"` on `DataTableShell` or `el-table`
- Card grids: `el-skeleton` with `:rows="6" animated`
- Buttons: `:loading="loading"` during async ops
- Forms: `v-loading` on specific sections

---

## 6. Animation & Interaction

### 6.1 Transition Durations

| Context | Duration | Easing |
|---------|----------|--------|
| Button hover | `150-200ms` | `ease` |
| Card hover (translate) | `200-300ms` | `ease` |
| Sidebar collapse | `300ms` | `ease-in-out` |
| Page transitions | `150-300ms` | `ease` |
| Modal open/close | `200-300ms` | `ease` |

### 6.2 Hover Effects

| Element | Effect | Implementation |
|---------|--------|----------------|
| Cards (clickable) | `hover:-translate-y-0.5 transition-all duration-200` | Tailwind classes |
| Cards (menu) | `hover:shadow-lg` | `shadow="hover"` on `el-card` |
| Sidebar collapse btn | `hover:bg-[--ep-menu-hover-bg-color] transition-colors duration-300 ease-in-out` | Inline styles |
| Table action links | Opacity `0.85` → `1` on row hover | SCSS in `index.scss` |
| Clickable table cells | Color change on hover | `.service-cell:hover .service-name` |

### 6.3 Interaction Rules

| Rule | Standard | Avoid |
|------|----------|-------|
| Button loading | Disable + spinner during async | No feedback |
| Toast duration | Auto dismiss 3-5 seconds | Permanent toasts |
| Modal click-outside | `close-on-click-modal: false` for forms | Accidental dismissal |
| Deletion confirmation | Always `ElMessageBox.confirm` | Instant deletion |
| Search | Debounce 300ms for input, Enter key for submit | No debounce |

### 6.4 NProgress

A progress bar at the top of the page for navigation:
```css
#nprogress .bar {
  background: rgb(13, 148, 136);
  height: 2px;
}
```

---

## 7. Iconography

### 7.1 Icon Sources

| Source | Usage |
|--------|-------|
| `@element-plus/icons-vue` | All UI controls, navigation, actions |
| `@iconify-json/codicon` via `icon-[codicon--*]` | Special icons (MCP, database) |
| SVG (`/public/dati.svg`) | Brand logo |

### 7.2 Icon Rules

| Rule | Standard | Avoid |
|------|----------|-------|
| Structural icons | Always use icon components (SVG-based) | Emoji as icons |
| Navigation icons | `el-icon` wrapping icon component | Raw icon text |
| Table action icons | Inline with `el-button link` | Icon-only without labels |
| Tooltip on icon-only | Always wrap with `el-tooltip` | Unlabeled icon buttons |
| Icon consistency | Same stroke weight within a hierarchy level | Mixing filled/outline |

### 7.3 Icon Sizing

| Context | Size |
|---------|------|
| Menu items | Default (`1em`) |
| Feature cards (home) | `text-4xl` (`36px`) |
| Action buttons | `size="default"` on `el-button` |
| Sidebar collapse button | `w-2 h-3` |
| Status indicator dot | `7px` circle |

---

## 8. Dark Mode

### 8.1 Implementation

Dark mode uses `@vueuse/core`'s `useDark()` composable with CSS variable switching via Element Plus dark SCSS.

**Configuration files:**
- `composables/dark.ts` — `useDark()` + `useToggle(isDark)`
- `styles/element/dark.scss` — Dark mode primary color override: `#589ef8`

### 8.2 Dark Mode Rules

| Rule | Standard | Avoid |
|------|----------|-------|
| Surface contrast | Use `--ep-bg-color` / `--ep-fill-color-*` automatically | Hardcoding light colors |
| Text contrast | `--ep-text-color-*` tokens auto-switch | Fixed color values |
| Primary color | Dark: `#589ef8` (set in dark.scss) | Same as light mode |
| Status colors | Test both modes independently | Assuming one mode works |

### 8.3 Current Gap

The primary color differs between light mode (`green`) and dark mode (`#589ef8` — blue). This should be unified for consistent brand identity.

---

## 9. Responsive Breakpoints

### 9.1 Breakpoints

| Name | Value | Target |
|------|-------|--------|
| `sm` | `640px` | Large phones |
| `md` | `768px` | Tablets portrait |
| `lg` | `1024px` | Tablets landscape |
| `xl` | `1280px` | Desktop |
| `2xl` | `1440px` | Large desktop |

### 9.2 Responsive Patterns in Code

| Pattern | Implementation |
|---------|---------------|
| Page padding | `p-5 md:p-6` |
| Stack controls vertically | `@media (max-width: 768px)` → `flex-col` |
| Sidebar collapse | Manual toggle at any size |
| MCP detail page | `grid` → `1fr` at `960px` and below |
| Basic info grid | `grid-cols-2` → `1fr` at `1200px` |
| Subject card grid | `auto-fill, minmax(320px, 1fr)` CSS grid |
| Search input width | `max-w-sm` on desktop, `w-full` on mobile |

### 9.3 Responsive Rules

| Rule | Standard | Avoid |
|------|----------|-------|
| Mobile-first | Default styles for mobile, override up | Desktop-only designs |
| Horizontal scroll | Ensure no overflow on 375px width | `overflow-x: auto` on containers |
| Touch targets | Min `44px` interactive area | Tiny click targets |
| Content priority | Show core content first, fold secondary | Hiding primary CTA on mobile |

---

## 10. Accessibility

### 10.1 Current Implementation

| Feature | Status | Notes |
|---------|--------|-------|
| Keyboard navigation | ✅ Element Plus built-in | Tab order matches visual |
| Form labels | ✅ Proper `<label>` via `el-form-item` | Uses `for` attribute |
| Alt text | ⚠️ Partial | Icons lack aria-labels |
| Color contrast | ✅ WCAG AA via Element Plus | Tested for default theme |
| Focus states | ✅ Element Plus defaults | Blue outline on focus |
| Screen reader | ✅ Element Plus aria attributes | Form validation, roles |
| Reduced motion | ❌ Not implemented | No `prefers-reduced-motion` handling |
| Skip links | ❌ Not implemented | Missing skip-to-content |

### 10.2 Required Improvements

| Priority | Action |
|----------|--------|
| High | Add `aria-label` to icon-only buttons (dark toggle, collapse, copy) |
| Medium | Add `prefers-reduced-motion` media query to disable animations |
| Medium | Add skip-to-main-content link |
| Low | Add descriptive `alt` text to decorative brand images |

---

## 11. Patterns & Templates

### 11.1 Page Type Templates

#### A. List Page (Table)
```html
<div class="p-5 md:p-6">
  <!-- Toolbar -->
  <div class="flex items-center justify-between gap-4 mb-6">
    <el-input v-model="keyword" class="max-w-sm" placeholder @keyup.enter @clear />
    <el-button type="primary" :icon="Plus" @click="create" />
  </div>
  <!-- Table -->
  <DataTableShell ...>
    <XxxTable :data="list" @edit @delete />
  </DataTableShell>
  <!-- Dialog -->
  <XxxDialog v-model="dialogVisible" ... />
</div>
```

#### B. List Page (Card Grid)
```html
<div class="space-y-4 p-5 md:p-6">
  <!-- Toolbar -->
  <div class="flex flex-wrap items-center justify-between gap-3">
    <el-input v-model="keyword" class="w-full md:max-w-sm" @keyup.enter @clear />
    <el-button type="primary" :icon="Plus" @click="create" />
  </div>
  <!-- Skeleton or Grid -->
  <el-skeleton v-if="loading" :rows="6" animated />
  <div v-else class="subject-grid">
    <SubjectCard v-for="item in list" :key="item.id" @click @edit @delete />
  </div>
  <!-- Pagination -->
  <div v-if="total > 0">
    <span>{{ t('common.total', { total }) }}</span>
    <el-pagination ... />
  </div>
</div>
```

#### C. Detail Page (Tabs)
```html
<div class="space-y-4 p-5 md:p-6">
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/parent' }">Parent</el-breadcrumb-item>
    <el-breadcrumb-item>{{ item.name }}</el-breadcrumb-item>
  </el-breadcrumb>

  <el-card shadow="never" class="border border-slate-200">
    <el-tabs>
      <el-tab-pane :label="t('xxx.basicInfo')">
        <!-- Content -->
      </el-tab-pane>
      <el-tab-pane :label="t('xxx.advanced')">
        <!-- Content -->
      </el-tab-pane>
    </el-tabs>
  </el-card>
</div>
```

#### D. Detail Page (Side Nav)
```html
<div class="detail-layout"> <!-- grid: 220-260px + 1fr -->
  <aside>
    <el-menu @select>
      <el-menu-item v-for="tab in tabs" :key="tab.key" :index="tab.key">
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
  <main>
    <div v-if="activeTab === 'basic'">
      <section class="panel">...</section>
    </div>
    <div v-else-if="activeTab === 'scope'">
      <div class="scope-panel">...</div>
    </div>
    <div v-else class="coming-soon">
      <el-empty />
    </div>
  </main>
</div>
```

### 11.2 Common Component Patterns

| Pattern | Files | Description |
|---------|-------|-------------|
| `*Form.vue` | `McpServiceForm`, `DatasourceForm` | Exposes `validate()`, `resetValidation()`, uses `ref` |
| `*Dialog.vue` | `McpServiceDialog`, `DatasourceDialog`, `SubjectDialog` | Manages CRUD logic, emits `success` |
| `*Table.vue` | `McpServiceTable`, `DatasourceTable` | Pure display + action emit, receives `data` + `loading` |
| `*Tab.vue` | `ToolsTab`, `PromptsTab`, `DataScopeTab` | Tab panel content, loads own data |
| `*List.vue` | `PrebuiltToolList`, `CustomToolList`, `PromptList` | Sub-list inside tab, receives data |

### 11.3 Data Loading Pattern

```ts
const loading = ref(false)
const data = ref<Type[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const resp = await api(page.value, pageSize.value, filters)
    data.value = resp.data || []
    total.value = resp.total ?? 0
  } catch {
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}
```

---

## 12. Contributing & Code Conventions

### 12.1 File Organization

```
src/
├── pages/          # Page (route) components
├── components/     # Reusable components grouped by feature
├── api/            # API calls + types
├── composables/    # Shared composable functions
├── stores/         # Pinia stores
├── locales/        # i18n messages
└── styles/         # Global styles
    ├── design-system/  # Design system documentation
    ├── element/        # Element Plus theme overrides
    ├── index.scss      # Global styles
    └── tailwind.css    # Tailwind import
```

### 12.2 Component Naming

| Pattern | Example | Rule |
|---------|---------|-------|
| Feature component | `McpServiceTable.vue` | PascalCase, feature-prefixed |
| Dialog | `SubjectDialog.vue` | Always `*Dialog.vue` |
| Form | `DatasourceForm.vue` | Always `*Form.vue` |
| Tab content | `ToolsTab.vue` | Always `*Tab.vue` |
| List | `PromptList.vue` | Sub-list inside tab |

### 12.3 Script Setup Conventions

```ts
<script setup lang="ts">
// 1. Vue/Router imports
import { ref, computed, onMounted } from 'vue'
// 2. UI library imports
import { ElMessage, ElMessageBox } from 'element-plus'
// 3. Icons
import { Plus, Search } from '@element-plus/icons-vue'
// 4. Composables
import { useI18n } from 'vue-i18n'
// 5. Store imports
import { useAuthStore } from '~/stores/auth'
// 6. API imports
import { listItems } from '~/api/feature'

const { t } = useI18n()

// 7. Props & Emits with interfaces
interface Props { ... }
interface Emits { ... }
const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 8. Reactive state
const loading = ref(false)

// 9. Computed

// 10. Methods (handle* prefix for event handlers)

// 11. onMounted / watchers
onMounted(() => { ... })
</script>
```

### 12.4 i18n Key Conventions

| Key Pattern | Example | Located In |
|-------------|---------|------------|
| `common.*` | `common.save`, `common.edit`, `common.delete` | Shared operations |
| `feature.*` | `datasource.createButton` | Feature-specific |
| `feature.status.*` | `mcpService.status.published` | Status labels |
| `feature.tool.*` | `mcpService.tool.sqlTemplate` | Sub-feature |
| `layout.*` | `layout.side.dataSources` | Layout components |
| `auth.*` | `auth.login` | Auth flow |
| `home.*` | `home.subtitle` | Landing page |

### 12.5 CSS Convention

| Context | Approach | Notes |
|---------|----------|-------|
| Global styles | `styles/index.scss` | Reset, element tweaks |
| Element Plus overrides | `styles/element/` | SCSS with `@forward` |
| Scoped component styles | `<style scoped>` | Tailwind + custom CSS |
| Tailwind utility | Inline classes | Prefer over custom CSS |
| CSS variables | `var(--ep-color-*)` | Never raw hex in templates |

---

## Appendix: Anti-pattern Checklist

- [ ] ❌ Emoji as structural icons (e.g., 🎨 🚀 in navigation)
- [ ] ❌ Raw hex colors in component templates (`#333`, `#f00`)
- [ ] ❌ Inconsistent page padding (mix of `p-4`, `p-6`, `p-5`)
- [ ] ❌ Disabled `close-on-click-modal` on non-form dialogs
- [ ] ❌ Missing loading state during async operations
- [ ] ❌ No confirmation on destructive actions (`handleDelete` bypasses `ElMessageBox`)
- [ ] ❌ Unlabeled icon-only buttons (no `el-tooltip` or `aria-label`)
- [ ] ❌ Placeholder-only form labels (must use `el-form-item` label)
- [ ] ❌ Fixed pixel widths for containers (`width: 800px`)
- [ ] ❌ Layout shift from images without declared dimensions
