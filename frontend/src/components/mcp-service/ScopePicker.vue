<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { Menu as IconMenu, Search } from "@element-plus/icons-vue";
import type { DataScopeItem } from "~/api/mcp-service";
import { listDataSources } from "~/api/datasource";
import { listSubjects } from "~/api/subject";

const { t } = useI18n();

const props = defineProps<{
  modelValue: boolean;
  existingItems: DataScopeItem[];
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "confirm", items: DataScopeItem[]): void;
}>();

type ScopeType = DataScopeItem["scope_type"];
type ScopeOption = {
  label: string;
  value: string;
  description?: string;
};

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});

const addType = ref<ScopeType>("DATA_SOURCE");
// 按类型分别维护选中（数据源/主题可同时选，互不覆盖）
const selectedByType = ref<Record<ScopeType, string[]>>({ DATA_SOURCE: [], SUBJECT: [] });
// 按类型分别缓存选项（切 Tab 后仍能取到另一 Tab 的 reference_name）
const optionsByType = ref<Record<ScopeType, ScopeOption[]>>({ DATA_SOURCE: [], SUBJECT: [] });

const dialogOptions = computed(() => optionsByType.value[addType.value]);
const currentSelected = computed(() => selectedByType.value[addType.value]);
const dialogLoading = ref(false);
const dialogPage = ref(1);
const dialogTotalCount = ref(0);
const dialogKeyword = ref("");
let searchTimer: ReturnType<typeof setTimeout> | null = null;
onUnmounted(() => {
  if (searchTimer) clearTimeout(searchTimer);
});

const PAGE_SIZE = 20;

// ── Dialog computed ──
const confirmCount = computed(
  () => selectedByType.value.DATA_SOURCE.length + selectedByType.value.SUBJECT.length,
);

const isAlreadyAdded = (refId: string) =>
  props.existingItems.some((i) => i.scope_type === addType.value && i.reference_id === refId);

const selectableOnPage = computed(() =>
  dialogOptions.value.filter((opt) => !isAlreadyAdded(opt.value)),
);

const allSelectableSelected = computed(() =>
  selectableOnPage.value.length > 0 &&
  selectableOnPage.value.every((opt) => currentSelected.value.includes(opt.value)),
);

const searchPlaceholder = computed(() =>
  addType.value === "DATA_SOURCE"
    ? t("mcpService.dataScope.searchDataSource")
    : t("mcpService.dataScope.searchSubject"),
);

// ── Data loading ──
const loadDialogOptions = async (page: number, keyword?: string) => {
  const requestType = addType.value; // 捕获请求时的类型，防止响应迟到时写错 Tab
  dialogLoading.value = true;
  try {
    if (requestType === "DATA_SOURCE") {
      const res = await listDataSources(page, PAGE_SIZE, keyword || undefined);
      optionsByType.value.DATA_SOURCE = (res.data || []).map((ds) => ({
        label: ds.name,
        value: ds.id,
        description: ds.description || ds.type,
      }));
      if (addType.value === requestType) dialogTotalCount.value = res.total;
    } else {
      const res = await listSubjects(page, PAGE_SIZE, keyword || undefined);
      optionsByType.value.SUBJECT = (res.data || []).map((s) => ({
        label: s.name,
        value: s.id,
        description: s.description || s.datasource_name || undefined,
      }));
      if (addType.value === requestType) dialogTotalCount.value = res.total;
    }
  } catch {
    if (addType.value === requestType) {
      optionsByType.value[requestType] = [];
      dialogTotalCount.value = 0;
    }
  } finally {
    if (addType.value === requestType) dialogLoading.value = false;
  }
};

const handleSearch = () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    dialogPage.value = 1;
    loadDialogOptions(1, dialogKeyword.value || undefined);
  }, 300);
};

const handleDialogPageChange = (page: number) => {
  dialogPage.value = page;
  loadDialogOptions(page, dialogKeyword.value || undefined);
};

const handleSelectAllToggle = () => {
  const target = addType.value;
  if (allSelectableSelected.value) {
    selectedByType.value[target] = currentSelected.value.filter(
      (id) => !selectableOnPage.value.some((opt) => opt.value === id),
    );
  } else {
    const toAdd = selectableOnPage.value
      .map((opt) => opt.value)
      .filter((id) => !currentSelected.value.includes(id));
    selectableOnPage.value.forEach((opt) => {
      labelByType.value[target][opt.value] = opt.label;
    });
    selectedByType.value[target] = [...currentSelected.value, ...toAdd];
  }
};

// 选中即记录 id→label（跨页选中也能拿到名称，不依赖分页缓存）
const labelByType = ref<Record<ScopeType, Record<string, string>>>({ DATA_SOURCE: {}, SUBJECT: {} });

const toggleSelection = (id: string) => {
  if (isAlreadyAdded(id)) return;
  const list = selectedByType.value[addType.value];
  const idx = list.indexOf(id);
  if (idx === -1) {
    const opt = dialogOptions.value.find((o) => o.value === id);
    if (opt) labelByType.value[addType.value][id] = opt.label;
    list.push(id);
  } else {
    list.splice(idx, 1);
  }
};

// ── Dialog actions ──
watch(visible, (v) => {
  if (v) {
    selectedByType.value = { DATA_SOURCE: [], SUBJECT: [] };
    labelByType.value = { DATA_SOURCE: {}, SUBJECT: {} };
    dialogKeyword.value = "";
    dialogPage.value = 1;
    loadDialogOptions(1);
  }
});

watch(addType, () => {
  // 不清空选中：数据源/主题可同时选，仅重置搜索与分页
  dialogKeyword.value = "";
  dialogPage.value = 1;
  loadDialogOptions(1);
});

const handleCancel = () => {
  visible.value = false;
};

const handleConfirm = () => {
  if (confirmCount.value === 0) return;
  // 合并两个 Tab 的选中，各自携带正确类型与名称
  const toItem = (type: ScopeType, id: string): DataScopeItem => {
    const label = labelByType.value[type][id]
      ?? optionsByType.value[type].find((o) => o.value === id)?.label;
    return label
      ? { scope_type: type, reference_id: id, reference_name: label }
      : { scope_type: type, reference_id: id };
  };
  const items: DataScopeItem[] = [
    ...selectedByType.value.DATA_SOURCE.map((id) => toItem("DATA_SOURCE", id)),
    ...selectedByType.value.SUBJECT.map((id) => toItem("SUBJECT", id)),
  ];
  emit("confirm", items);
  visible.value = false;
};
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('mcpService.dataScope.addDialogTitle')"
    width="780px"
    :close-on-click-modal="false"
    class="scope-add-dialog"
  >
    <!-- Tabs + Search -->
    <div class="mb-4 flex items-center justify-between gap-4">
      <div class="flex gap-1 rounded-lg bg-[var(--ep-fill-color)] p-1">
        <button
          class="tab-btn"
          :class="{ active: addType === 'DATA_SOURCE' }"
          @click="addType = 'DATA_SOURCE'"
        >
          <el-icon><span class="icon-[codicon--database]"></span></el-icon>
          {{ t("common.dataSource") }}
        </button>
        <button
          class="tab-btn"
          :class="{ active: addType === 'SUBJECT' }"
          @click="addType = 'SUBJECT'"
        >
          <el-icon><IconMenu /></el-icon>
          {{ t("common.subject") }}
        </button>
      </div>
      <el-input
        v-model="dialogKeyword"
        class="w-64"
        :placeholder="searchPlaceholder"
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
        @clear="handleSearch"
      />
    </div>

    <DataTableShell
      compact
      :loading="dialogLoading"
      :total="dialogTotalCount"
      :page="dialogPage"
      :page-size="PAGE_SIZE"
      :page-sizes="[]"
      @page-change="handleDialogPageChange"
    >
      <!-- Table header -->
      <div class="table-header">
        <span class="col-check">
          <el-checkbox
            :model-value="allSelectableSelected"
            :disabled="dialogLoading || selectableOnPage.length === 0"
            @change="handleSelectAllToggle"
          />
        </span>
        <span class="col-name">{{ t("common.name") }}</span>
        <span class="col-desc">{{ t("common.description") }}</span>
        <span class="col-status"></span>
      </div>

      <!-- Table body -->
      <div class="min-h-64">
        <el-empty
          v-if="!dialogLoading && dialogOptions.length === 0"
          :description="t('mcpService.dataScope.noResults')"
          :image-size="80"
        />

        <div
          v-for="opt in dialogOptions"
          :key="opt.value"
          class="table-row"
          :class="{
            existing: isAlreadyAdded(opt.value),
            selected: !isAlreadyAdded(opt.value) && currentSelected.includes(opt.value),
          }"
          @click="!isAlreadyAdded(opt.value) && toggleSelection(opt.value)"
        >
          <span class="col-check" @click.stop>
            <el-checkbox
              :model-value="isAlreadyAdded(opt.value) || currentSelected.includes(opt.value)"
              :disabled="isAlreadyAdded(opt.value)"
              @change="toggleSelection(opt.value)"
            />
          </span>
          <span class="col-name">
            <span class="row-title">{{ opt.label }}</span>
            <span class="row-id">{{ opt.value }}</span>
          </span>
          <span class="col-desc">{{ opt.description || "" }}</span>
          <span class="col-status">
            <el-tag v-if="isAlreadyAdded(opt.value)" size="small" type="info">
              {{ t("mcpService.dataScope.alreadyAdded") }}
            </el-tag>
            <el-tag
              v-else-if="currentSelected.includes(opt.value)"
              size="small"
              type="primary"
              effect="plain"
            >
              {{ t("mcpService.dataScope.selected") }}
            </el-tag>
          </span>
        </div>
      </div>
    </DataTableShell>

    <!-- Footer -->
    <template #footer>
      <div class="flex items-center justify-between gap-3 w-full">
        <span class="text-sm text-[var(--ep-text-color-secondary)]">
          {{ confirmCount > 0 ? t("mcpService.dataScope.selectedCount", { count: confirmCount }) : t("mcpService.dataScope.noSelection") }}
        </span>
        <div class="flex items-center gap-3">
          <el-button @click="handleCancel">
            {{ t("common.cancel") }}
          </el-button>
          <el-button
            type="primary"
            :disabled="confirmCount === 0"
            @click="handleConfirm"
          >
            {{ t("mcpService.dataScope.confirmAdd", { count: confirmCount }) }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ep-text-color-secondary);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;
}
.tab-btn:hover {
  color: var(--ep-text-color-primary);
}
.tab-btn.active {
  background: var(--ep-bg-color);
  color: var(--ep-color-primary);
  box-shadow: 0 1px 3px rgb(0 0 0 / 8%);
}

.table-header {
  display: grid;
  grid-template-columns: 44px minmax(0, 2fr) minmax(0, 2.5fr) 90px;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--ep-border-color-lighter);
  color: var(--ep-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.table-row {
  display: grid;
  grid-template-columns: 44px minmax(0, 2fr) minmax(0, 2.5fr) 90px;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--ep-border-color-lighter);
  cursor: pointer;
  transition: background 0.1s;
}
.table-row:hover {
  background: var(--ep-fill-color-lighter);
}
.table-row.existing {
  background: var(--ep-fill-color-lighter);
  opacity: 0.65;
  cursor: not-allowed;
}
.table-row.selected {
  background: var(--ep-color-primary-light-9);
}
.table-row:last-child {
  border-bottom: 0;
}

.col-check {
  display: flex;
  justify-content: center;
}
.col-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.col-status {
  display: flex;
  justify-content: flex-end;
}

.row-title {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ep-text-color-primary);
}
.row-id {
  overflow: hidden;
  font-family: "SF Mono", "Cascadia Code", monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ep-text-color-placeholder);
}
.col-desc {
  overflow: hidden;
  padding-right: 8px;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ep-text-color-secondary);
}

@media (max-width: 720px) {
  .table-header,
  .table-row {
    grid-template-columns: 40px minmax(0, 1fr) 80px;
  }
  .col-desc {
    display: none;
  }
}
</style>
