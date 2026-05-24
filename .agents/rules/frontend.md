# Frontend Coding Conventions

## Directory Structure

```
src/
├── pages/                  # File-based routing (unplugin-vue-router)
│   └── <feature>/[id]/     # Dynamic route params: [id].vue
├── components/<feature>/   # Auto-imported, grouped by domain
├── api/
│   ├── http.ts             # Axios instance, interceptors, typed wrappers (get/post/put/del)
│   ├── types.ts            # Shared: IdResponse, PageResponse, BaseResourceVO, User
│   └── <feature>.ts        # Domain APIs + local VO/Request types
├── composables/            # Reusable composables, barrel-exported via index.ts
├── stores/                 # Pinia (setup syntax)
├── locales/                # i18n (zh.ts, en.ts)
├── styles/                 # TailwindCSS 4 + Element Plus SCSS overrides
└── plugins/                # i18n setup
```

**Path alias**: `~/` → `src/`. All imports use `~/api/...`, `~/components/...`, `~/stores/...`.

**Vite proxy**: `/api` → `http://localhost:8085` (strips `/api` prefix).

**Auto-imports**: `vue`/`vue-router` APIs and `src/components/` are auto-imported. Element Plus components auto-imported with sass on-demand.

## API Layer

All HTTP calls use the typed wrappers from `api/http.ts`:

```ts
import { get, post, put, del } from './http'
// get<T>(url, params?, signal?) → Promise<T>
// post<T, B>(url, body?, signal?) → Promise<T>
// put<T, B>(url, body?, signal?) → Promise<T>
// del<T>(url, params?, signal?) → Promise<T>
```

**Key behaviors**:
- Response interceptor returns `resp.data` directly
- `Authorization: Bearer <token>` injected from localStorage
- 401 on non-auth URLs → redirect `/login`

**Domain API conventions**:
- VO types extend `BaseResourceVO`; field names `snake_case`
- `Create*Request` / `Update*Request` types co-located in the same file
- URL path params use `encodeURIComponent()`
- Functions accept optional `signal?: AbortSignal`

```ts
interface FooVO extends BaseResourceVO { ... }
interface CreateFooRequest { ... }

export function listFoos(page: number, size: number, keyword?: string, signal?: AbortSignal): Promise<PageResponse<FooVO>>
export function createFoo(body: CreateFooRequest, signal?: AbortSignal): Promise<IdResponse>
export function updateFoo(id: string, body: UpdateFooRequest, signal?: AbortSignal): Promise<IdResponse>
export function deleteFoo(id: string, signal?: AbortSignal): Promise<IdResponse>
```

## Components

Always `<script setup lang="ts">`. Components use `PascalCase` in templates.

**Props/Emits** — always TypeScript interface syntax, not runtime option objects:
```ts
interface Props { modelValue: boolean; item?: FooVO | null }
interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}
defineProps<Props>()
defineEmits<Emits>()
```

**Dialog v-model** — use `computed` get/set:
```ts
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})
```

**Form validation** — always use Element Plus `FormRules`, never write custom validation logic. Form ref typed as `FormInstance`, expose `validate()` / `resetValidation()` via `defineExpose`.

**Edit mode** — watch the item prop to populate/reset form:
```ts
watch(() => props.item, (val) => {
  if (val) formData.value = { ...val }
  else resetForm()
}, { immediate: true })
```

## Pages

### Standard CRUD Pattern

```ts
const loading = ref(false)
const list = ref<T[]>([])
const dialogVisible = ref(false)
const currentItem = ref<T | null>(null)
const searchKeyword = ref('')
const page = ref(1); const pageSize = ref(10); const total = ref(0)

const loadData = async () => {
  loading.value = true
  try {
    const res = await listApi(page.value, pageSize.value, searchKeyword.value)
    list.value = res.data || []; total.value = res.total ?? 0
  } catch (e) { console.error(e); ElMessage.error(t('common.loadFailed')) }
  finally { loading.value = false }
}

// Search resets to page 1; pagination events trigger reload
const handleSearch = () => { page.value = 1; loadData() }
const handleCreate = () => { currentItem.value = null; dialogVisible.value = true }
const handleEdit = (item: T) => { currentItem.value = { ...item }; dialogVisible.value = true }
const handleDialogSuccess = () => { dialogVisible.value = false; loadData() }
```

### Delete Confirmation

Always use `ElMessageBox.confirm` with `type: 'warning'`. Check `error !== 'cancel'` before showing error messages.

### File-based Routing

Pages use `<route lang="yaml">` for route meta:
```vue
<route lang="yaml">
meta:
  activeMenu: /subjects
</route>
```

**Typed route params**: `const route = useRoute('/datasources/[id]/tables/')` gives typed `route.params.id`.

## State Management (Pinia)

Setup syntax only. Pattern:
```ts
export const useXxxStore = defineStore('xxx', () => {
  const data = ref<T | null>(null)
  const loading = ref(false)

  const isLoaded = computed(() => data.value !== null)

  const load = async () => {
    loading.value = true
    try { data.value = await api() }
    catch (e) { console.error(e) }
    finally { loading.value = false }
  }

  return { data, loading, isLoaded, load }
})
```

## Routing

**Auth guard** in `main.ts`: public paths are `/login` and `/register`. Authenticated users on public paths redirect to `/`, unauthenticated users elsewhere redirect to `/login`.

## i18n

- Lazy-loaded locale files, language persists in localStorage
- Usage: `const { t } = useI18n()` → `t('common.cancel')`, `t('common.confirmDelete', { name })`
- Key naming: nested by feature — `common.*`, `auth.*`, `datasource.*`, `subject.*`, `tableInfo.*`, `layout.*`

## Styling

- **TailwindCSS 4** atomic classes first (`flex items-center gap-4 p-5`)
- **Scoped styles** only for overrides that Tailwind can't express
- Element Plus theme via CSS vars: `var(--ep-bg-color)`, `var(--ep-fill-color-light)`, etc.
- Dark mode via `@vueuse/core` (`useDark`/`useToggle`), exported from `~/composables/dark.ts`

## Layout

- Auth pages (`/login`, `/register`): `RouterView` directly, no layout
- All other pages: `BaseHeader` + `BaseSide` + `RouterView`
- Sidebar active state: driven by route `meta.activeMenu`
- Height: `calc(100vh - var(--ep-menu-item-height) - 4px)`

## Error Handling

Every async operation: try/catch with `console.error` + `ElMessage.error`. Always `finally { loading.value = false }`.

## Key Rules Summary

| Rule | Detail |
|------|--------|
| SFC | `<script setup lang="ts">` |
| Component naming | `PascalCase` in templates |
| JSON vs local types | `snake_case` for API, `camelCase` for local |
| HTTP | Only via `get/post/put/del` from `api/http.ts` |
| State | Pinia setup syntax, `ref`+`computed`+async |
| Styling | TailwindCSS 4 atomic classes |
| I18n | `t('key')` for all user-facing text |
| Props/Emits | TypeScript interfaces, not runtime option objects |
| Dialogs | `v-model` with computed get/set |
| Form validation | Element Plus `FormRules`, no custom validation |
| Deletion | `ElMessageBox.confirm` + `error !== 'cancel'` guard |
| Pagination | Search resets page to 1 |
