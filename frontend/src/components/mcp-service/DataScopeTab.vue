<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Delete, OfficeBuilding, Collection, Search } from "@element-plus/icons-vue";
import type { DataScopeItem } from "~/api/mcp-service";
import { getDataScope, saveDataScope } from "~/api/mcp-service";
import { listDataSources } from "~/api/datasource";
import { listSubjects } from "~/api/subject";

const { t } = useI18n();

const props = defineProps<{
  serviceId: string;
  serviceStatus?: string;
}>();

const emit = defineEmits<{ (e: "refresh"): void }>();

type ScopeType = DataScopeItem["scope_type"];
type ScopeOption = {
  label: string;
  value: string;
  description?: string;
};

const loading = ref(false);
const saving = ref(false);
const items = ref<DataScopeItem[]>([]);

// ── Dialog state ──
const addDialogVisible = ref(false);
const addType = ref<ScopeType>("DATA_SOURCE");
const selectedIds = ref<string[]>([]);

const dialogOptions = ref<ScopeOption[]>([]);
const dialogLoading = ref(false);
const dialogPage = ref(1);
const dialogTotalCount = ref(0);
const dialogKeyword = ref("");
let searchTimer: ReturnType<typeof setTimeout> | null = null;
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer); });

const PAGE_SIZE = 20;

// ── Dialog computed ──
const confirmCount = computed(() => selectedIds.value.length);

const isAlreadyAdded = (refId: string) =>
  items.value.some((i) => i.scope_type === addType.value && i.reference_id === refId);

const selectableOnPage = computed(() =>
  dialogOptions.value.filter((opt) => !isAlreadyAdded(opt.value)),
);

const allSelectableSelected = computed(() =>
  selectableOnPage.value.length > 0 &&
  selectableOnPage.value.every((opt) => selectedIds.value.includes(opt.value)),
);

const searchPlaceholder = computed(() =>
  addType.value === "DATA_SOURCE"
    ? t("mcpService.dataScope.searchDataSource")
    : t("mcpService.dataScope.searchSubject"),
);

// ── Data loading ──
const loadDataScope = async () => {
  loading.value = true;
  try {
    const data = await getDataScope(props.serviceId);
    items.value = data.items || [];
  } finally {
    loading.value = false;
  }
};

const loadDialogOptions = async (page: number, keyword?: string) => {
  dialogLoading.value = true;
  try {
    if (addType.value === "DATA_SOURCE") {
      const res = await listDataSources(page, PAGE_SIZE, keyword || undefined);
      dialogOptions.value = (res.data || []).map((ds) => ({
        label: ds.name,
        value: ds.id,
        description: ds.description || ds.type,
      }));
      dialogTotalCount.value = res.total;
    } else {
      const res = await listSubjects(page, PAGE_SIZE, keyword || undefined);
      dialogOptions.value = (res.data || []).map((s) => ({
        label: s.name,
        value: s.id,
        description: s.description || s.datasource_name || undefined,
      }));
      dialogTotalCount.value = res.total;
    }
  } catch {
    dialogOptions.value = [];
    dialogTotalCount.value = 0;
  } finally {
    dialogLoading.value = false;
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
  if (allSelectableSelected.value) {
    selectedIds.value = selectedIds.value.filter(
      (id) => !selectableOnPage.value.some((opt) => opt.value === id),
    );
  } else {
    const toAdd = selectableOnPage.value
      .map((opt) => opt.value)
      .filter((id) => !selectedIds.value.includes(id));
    selectedIds.value = [...selectedIds.value, ...toAdd];
  }
};

const toggleSelection = (id: string) => {
  if (isAlreadyAdded(id)) return;
  const idx = selectedIds.value.indexOf(id);
  if (idx === -1) {
    selectedIds.value.push(id);
  } else {
    selectedIds.value.splice(idx, 1);
  }
};

// ── Dialog actions ──
const handleOpenAddDialog = () => {
  selectedIds.value = [];
  dialogKeyword.value = "";
  dialogPage.value = 1;
  addDialogVisible.value = true;
  loadDialogOptions(1);
};

const handleConfirmAdd = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning(t("mcpService.dataScope.selectFirst"));
    return;
  }

  const nextItems: DataScopeItem[] = selectedIds.value
    .map((id) => {
      const opt = dialogOptions.value.find((o) => o.value === id);
      return opt
        ? ({ scope_type: addType.value, reference_id: opt.value } as DataScopeItem)
        : null;
    })
    .filter((item): item is DataScopeItem => item !== null);

  if (nextItems.length === 0) return;

  saving.value = true;
  try {
    await saveDataScope(props.serviceId, { items: [...items.value, ...nextItems] });
    await loadDataScope();
    addDialogVisible.value = false;
    emit("refresh");
  } catch (error) {
    console.error("Failed to add data scope:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};

// ── List actions ──
const handleRemove = async (index: number) => {
  try {
    await ElMessageBox.confirm(t("mcpService.dataScope.deleteConfirm"), t("common.confirm"), {
      confirmButtonText: t("common.confirm"),
      cancelButtonText: t("common.cancel"),
      type: "warning",
    });
  } catch {
    return;
  }

  saving.value = true;
  try {
    await saveDataScope(props.serviceId, { items: items.value.filter((_, i) => i !== index) });
    await loadDataScope();
    emit("refresh");
  } finally {
    saving.value = false;
  }
};

// ── Labels ──
const scopeTypeLabel = (type: ScopeType) =>
  type === "DATA_SOURCE"
    ? t("common.dataSource")
    : t("common.subject");

watch(addType, () => {
  selectedIds.value = [];
  dialogKeyword.value = "";
  dialogPage.value = 1;
  loadDialogOptions(1);
});

onMounted(() => {
  loadDataScope();
});
</script>

<template>
  <div v-loading="loading" class="flex flex-col gap-4">
    <!-- Header -->
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="text-base font-semibold text-[var(--ep-text-color-primary)]">{{ t("mcpService.tab.dataScope") }}</h2>
        <span class="text-xs text-[var(--ep-text-color-secondary)]">{{ t("mcpService.dataScope.subtitle") }}</span>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleOpenAddDialog">
        {{ t("mcpService.dataScope.addScope") }}
      </el-button>
    </div>

    <!-- Published hint -->
    <el-alert
      v-if="serviceStatus === 'PUBLISHED'"
      :title="t('mcpService.dataScope.publishedHint')"
      type="warning"
      :closable="false"
      show-icon
    />

    <!-- Empty -->
    <el-empty v-if="items.length === 0" :description="t('mcpService.dataScope.empty')" />

    <!-- Scope list -->
    <div v-else class="flex flex-col gap-2">
      <div
        v-for="(item, index) in items"
        :key="item.id || `${item.scope_type}-${item.reference_id}-${index}`"
        class="scope-item flex items-center justify-between gap-4 rounded-lg border border-[var(--ep-border-color-lighter)] bg-[var(--ep-bg-color)] px-4 py-3"
      >
        <div class="flex min-w-0 items-center gap-3">
          <el-icon
            class="scope-icon"
            :class="item.scope_type === 'SUBJECT' ? 'subject' : ''"
          >
            <OfficeBuilding v-if="item.scope_type === 'DATA_SOURCE'" />
            <Collection v-else />
          </el-icon>
          <div class="flex min-w-0 flex-col gap-1.5">
            <div class="flex min-w-0 items-center gap-2">
              <span class="truncate text-sm font-semibold text-[var(--ep-text-color-primary)]">
                {{ item.reference_name }}
              </span>
              <el-tag size="small" :type="item.scope_type === 'DATA_SOURCE' ? 'primary' : 'success'">
                {{ scopeTypeLabel(item.scope_type) }}
              </el-tag>
            </div>
            <div class="flex min-w-0 items-center gap-2 text-xs text-[var(--ep-text-color-secondary)]">
              <span class="truncate">{{ item.reference_id }}</span>
            </div>
          </div>
        </div>
        <el-button link type="danger" :icon="Delete" @click="handleRemove(index)">
          {{ t("common.delete") }}
        </el-button>
      </div>
    </div>

    <!-- ── Add Dialog ── -->
    <el-dialog
      v-model="addDialogVisible"
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
            <el-icon><OfficeBuilding /></el-icon>
            {{ t("common.dataSource") }}
          </button>
          <button
            class="tab-btn"
            :class="{ active: addType === 'SUBJECT' }"
            @click="addType = 'SUBJECT'"
          >
            <el-icon><Collection /></el-icon>
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
              selected: !isAlreadyAdded(opt.value) && selectedIds.includes(opt.value),
            }"
            @click="!isAlreadyAdded(opt.value) && toggleSelection(opt.value)"
          >
            <span class="col-check" @click.stop>
              <el-checkbox
                :model-value="isAlreadyAdded(opt.value) || selectedIds.includes(opt.value)"
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
                v-else-if="selectedIds.includes(opt.value)"
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
            <el-button @click="addDialogVisible = false">
              {{ t("common.cancel") }}
            </el-button>
            <el-button
              type="primary"
              :disabled="confirmCount === 0"
              :loading="saving"
              @click="handleConfirmAdd"
            >
              {{ t("mcpService.dataScope.confirmAdd", { count: confirmCount }) }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.scope-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--ep-fill-color-lighter);
  color: var(--ep-color-primary);
}
.scope-icon.subject {
  color: var(--ep-color-success);
}

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
