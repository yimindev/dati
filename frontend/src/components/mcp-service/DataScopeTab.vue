<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Delete, Edit, Menu as IconMenu, CopyDocument } from "@element-plus/icons-vue";
import type { DataScopeAccessMode, DataScopeItem } from "~/api/mcp-service";
import { getDataScope, saveDataScope } from "~/api/mcp-service";
import { notifyError } from "~/api/http";
import { allowedSchemasOf, isManagedSchemaMode } from "~/utils/data-scope";
import { copyToClipboard } from "~/utils/clipboard";

const { t } = useI18n();

const props = defineProps<{
  serviceId: string;
}>();

const emit = defineEmits<{ (e: "refresh"): void }>();

type ScopeType = DataScopeItem["scope_type"];

const loading = ref(false);
const saving = ref(false);
const items = ref<DataScopeItem[]>([]);

// ── Add dialog state（选择器已抽为 ScopePicker 组件）──
const addDialogVisible = ref(false);

// ── Access mode dialog state ──
const accessDialogVisible = ref(false);
const accessDialogIndex = ref<number | null>(null);
const accessDialogItem = computed(() =>
  accessDialogIndex.value == null ? null : (items.value[accessDialogIndex.value] ?? null),
);

// ── Data loading ──
const loadDataScope = async () => {
  loading.value = true;
  try {
    const data = await getDataScope(props.serviceId);
    items.value = data.items || [];
  } catch (error) {
    console.error("Failed to load data scope:", error);
    notifyError(error, t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const persistItems = async (nextItems: DataScopeItem[]): Promise<boolean> => {
  saving.value = true;
  try {
    await saveDataScope(props.serviceId, { items: nextItems });
    await loadDataScope();
    emit("refresh");
    return true;
  } catch (error) {
    console.error("Failed to save data scope:", error);
    notifyError(error, t("common.operationFailed"));
    return false;
  } finally {
    saving.value = false;
  }
};

// ── Dialog actions ──
const handleConfirmAdd = async (newItems: DataScopeItem[]) => {
  if (newItems.length === 0) return;

  const additions: DataScopeItem[] = newItems.map((item) => ({
    ...item,
    access_mode:
      item.scope_type === "DATA_SOURCE" ? ("IMPORTED_TABLES" as const) : undefined,
    allowed_schemas: [],
  }));

  const saved = await persistItems([...items.value, ...additions]);
  if (saved) {
    addDialogVisible.value = false;
  }
};

const openAccessDialog = (index: number) => {
  accessDialogIndex.value = index;
  accessDialogVisible.value = true;
};

const handleAccessConfirm = async (payload: {
  access_mode: DataScopeAccessMode;
  allowed_schemas: string[];
}) => {
  const index = accessDialogIndex.value;
  if (index == null) return;

  const nextItems = items.value.map((item, i) =>
    i === index
      ? {
          ...item,
          access_mode: payload.access_mode,
          allowed_schemas: payload.allowed_schemas,
        }
      : item,
  );

  const saved = await persistItems(nextItems);
  if (saved) {
    accessDialogVisible.value = false;
    accessDialogIndex.value = null;
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

  await persistItems(items.value.filter((_, i) => i !== index));
};

// ── Labels ──
const scopeTypeLabel = (type: ScopeType) =>
  type === "DATA_SOURCE"
    ? t("common.dataSource")
    : t("common.subject");

const accessSchemas = (item: DataScopeItem) => allowedSchemasOf(item);

const handleCopyId = async (id: string) => {
  const success = await copyToClipboard(id);
  if (success) {
    ElMessage.success(t("common.copySuccess"));
  } else {
    ElMessage.error(t("common.copyFailed"));
  }
};


watch(accessDialogVisible, (open) => {
  if (!open) accessDialogIndex.value = null;
});

onMounted(() => {
  loadDataScope();
});
</script>

<template>
  <div v-loading="loading" class="flex flex-col gap-4">
    <!-- Action Header Line -->
    <div class="flex items-center justify-end pb-2 border-b border-[var(--ep-border-color-lighter)]">
      <el-button type="primary" :icon="Plus" :disabled="saving" @click="addDialogVisible = true">
        {{ t("mcpService.dataScope.addScope") }}
      </el-button>
    </div>

    <!-- Empty -->
    <el-empty v-if="items.length === 0" :description="t('mcpService.dataScope.empty')" />

    <!-- Scope list -->
    <div v-else class="flex flex-col gap-2">
      <div
        v-for="(item, index) in items"
        :key="item.id || `${item.scope_type}-${item.reference_id}-${index}`"
        class="scope-item flex flex-wrap items-start justify-between gap-3 rounded-lg border border-[var(--ep-border-color-lighter)] bg-[var(--ep-bg-color)] px-4 py-3"
      >
        <div class="flex min-w-0 flex-1 items-start gap-3">
          <el-icon
            class="scope-icon"
            :class="item.scope_type === 'SUBJECT' ? 'subject' : ''"
          >
            <span v-if="item.scope_type === 'DATA_SOURCE'" class="icon-[codicon--database]"></span>
            <IconMenu v-else />
          </el-icon>
          <div class="flex min-w-0 flex-col gap-1.5">
            <div class="flex min-w-0 flex-wrap items-center gap-2">
              <span class="truncate text-sm font-semibold text-[var(--ep-text-color-primary)]">
                {{ item.reference_name }}
              </span>
              <el-tag size="small" :type="item.scope_type === 'DATA_SOURCE' ? 'primary' : 'success'">
                {{ scopeTypeLabel(item.scope_type) }}
              </el-tag>
            </div>
            <div class="flex min-w-0 items-center gap-1.5 text-xs text-[var(--ep-text-color-placeholder)]">
              <span class="font-mono text-[11px] leading-none">{{ item.reference_id }}</span>
              <button
                type="button"
                class="inline-flex items-center justify-center p-0.5 text-[var(--ep-text-color-placeholder)] hover:text-[var(--ep-color-primary)] bg-transparent border-0 cursor-pointer rounded transition-colors"
                :title="t('common.copySuccess')"
                @click="handleCopyId(item.reference_id)"
              >
                <el-icon :size="12"><CopyDocument /></el-icon>
              </button>
            </div>
            <div class="flex min-w-0 flex-wrap items-center gap-1.5 text-xs text-[var(--ep-text-color-secondary)]">
              <span>{{ t("mcpService.dataScope.accessMode") }}：</span>
              <!-- DATA_SOURCE scope: interactive configuration pill -->
              <template v-if="item.scope_type === 'DATA_SOURCE'">
                <button
                  type="button"
                  class="scope-access-pill group"
                  :class="{ managed: isManagedSchemaMode(item) }"
                  :title="t('mcpService.dataScope.clickToConfig')"
                  @click="openAccessDialog(index)"
                >
                  <span class="pill-text">{{ isManagedSchemaMode(item) ? t("mcpService.dataScope.modeManaged") : t("mcpService.dataScope.modeImported") }}</span>
                  <span v-if="isManagedSchemaMode(item) && accessSchemas(item).length > 0" class="schema-badge">
                    {{ accessSchemas(item).join(", ") }}
                  </span>
                  <el-icon class="pill-icon"><Edit /></el-icon>
                </button>
              </template>
              <!-- SUBJECT scope: static informational badge -->
              <template v-else>
                <el-tooltip :content="t('mcpService.dataScope.subjectScopeDesc')" placement="top">
                  <span class="scope-access-pill static">
                    <span class="pill-text">{{ t("mcpService.dataScope.subjectScope") }}</span>
                  </span>
                </el-tooltip>
              </template>
            </div>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-1">
          <el-button
            link
            type="danger"
            :icon="Delete"
            :disabled="saving"
            @click="handleRemove(index)"
          >
            {{ t("common.delete") }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- ── ScopePicker (shared selector) ── -->
    <ScopePicker
      v-model="addDialogVisible"
      :existing-items="items"
      @confirm="handleConfirmAdd"
    />

    <!-- ── Access mode dialog ── -->
    <ScopeAccessDialog
      v-model="accessDialogVisible"
      :item="accessDialogItem"
      :saving="saving"
      @confirm="handleAccessConfirm"
    />
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

.scope-access-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid var(--ep-border-color-light);
  background-color: var(--ep-fill-color-blank);
  color: var(--ep-text-color-regular);
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scope-access-pill:hover {
  border-color: var(--ep-color-primary);
  color: var(--ep-color-primary);
  background-color: var(--ep-color-primary-light-9);
}

.scope-access-pill.managed {
  border-color: var(--ep-color-primary-light-5);
  background-color: var(--ep-color-primary-light-9);
  color: var(--ep-color-primary-dark-2);
}

.scope-access-pill.managed:hover {
  border-color: var(--ep-color-primary);
  background-color: var(--ep-color-primary-light-8);
}

.scope-access-pill.static {
  cursor: default;
  border-color: var(--ep-border-color-lighter);
  background-color: var(--ep-fill-color-light);
  color: var(--ep-text-color-secondary);
}


.pill-text {
  font-weight: 500;
}

.pill-icon {
  font-size: 11px;
  color: var(--ep-text-color-placeholder);
  transition: color 0.2s;
}

.scope-access-pill:hover .pill-icon {
  color: var(--ep-color-primary);
}

.schema-badge {
  padding: 0 4px;
  border-radius: 4px;
  background-color: var(--ep-bg-color);
  border: 1px solid var(--ep-color-primary-light-7);
  color: var(--ep-color-primary);
  font-family: var(--ep-font-family-mono, monospace);
  font-size: 11px;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
