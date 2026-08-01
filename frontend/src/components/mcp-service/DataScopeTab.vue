<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Delete, Menu as IconMenu } from "@element-plus/icons-vue";
import type { DataScopeItem } from "~/api/mcp-service";
import { getDataScope, saveDataScope } from "~/api/mcp-service";

const { t } = useI18n();

const props = defineProps<{
  serviceId: string;
  serviceStatus?: string;
}>();

const emit = defineEmits<{ (e: "refresh"): void }>();

type ScopeType = DataScopeItem["scope_type"];

const loading = ref(false);
const saving = ref(false);
const items = ref<DataScopeItem[]>([]);

// ── Add dialog state（选择器已抽为 ScopePicker 组件）──
const addDialogVisible = ref(false);

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

// ── Dialog actions ──
const handleConfirmAdd = async (newItems: DataScopeItem[]) => {
  if (newItems.length === 0) return;

  saving.value = true;
  try {
    await saveDataScope(props.serviceId, { items: [...items.value, ...newItems] });
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
      <el-button type="primary" :icon="Plus" @click="addDialogVisible = true">
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
            <span v-if="item.scope_type === 'DATA_SOURCE'" class="icon-[codicon--database]"></span>
            <IconMenu v-else />
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

    <!-- ── ScopePicker (shared selector) ── -->
    <ScopePicker
      v-model="addDialogVisible"
      :existing-items="items"
      @confirm="handleConfirmAdd"
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
</style>
