<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { Edit, Delete, Plus, Search, Coin, CaretRight } from "@element-plus/icons-vue";
import type { McpToolVO } from "~/api/mcp-tool";
import { updateTool, deleteCustomTool } from "~/api/mcp-tool";
import { getDataScope } from "~/api/mcp-service";
import CustomToolDialog from "./CustomToolDialog.vue";
import ToolTestDialog from "./ToolTestDialog.vue";

const { t } = useI18n();

const props = defineProps<{ tools: McpToolVO[]; serviceId: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();

const searchQuery = ref("");
const dialogVisible = ref(false);
const editingTool = ref<McpToolVO | null>(null);
const dsNameMap = ref<Record<string, string>>({});
const testVisible = ref(false);
const testingTool = ref<McpToolVO | null>(null);
const openTest = (tool: McpToolVO) => {
  testingTool.value = tool;
  testVisible.value = true;
};

onMounted(async () => {
  try {
    const resp = await getDataScope(props.serviceId);
    for (const item of resp.items || []) {
      if (item.scope_type === "DATA_SOURCE") {
        dsNameMap.value[item.reference_id] = item.reference_name ?? item.reference_id;
      }
    }
  } catch { /* ignore */ }
});

const filteredTools = computed(() => {
  const q = searchQuery.value.toLowerCase();
  if (!q) return props.tools;
  return props.tools.filter(
    (t) =>
      t.name.toLowerCase().includes(q) ||
      (t.title || "").toLowerCase().includes(q) ||
      (t.description || "").toLowerCase().includes(q),
  );
});

const handleToggle = async (tool: McpToolVO) => {
  try {
    await updateTool(props.serviceId, tool.id, { tool_type: tool.tool_type, enabled: !tool.enabled });
    ElMessage.success(t("common.saveSuccess"));
    emit("refresh");
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  }
};

const handleCreate = () => {
  editingTool.value = null;
  dialogVisible.value = true;
};

const handleEdit = (tool: McpToolVO) => {
  editingTool.value = { ...tool };
  dialogVisible.value = true;
};

const handleDelete = async (tool: McpToolVO) => {
  try {
    await ElMessageBox.confirm(
      t("mcpService.tool.deleteConfirm"),
      t("common.warning"),
      { confirmButtonText: t("common.confirm"), cancelButtonText: t("common.cancel"), type: "warning" },
    );
    await deleteCustomTool(props.serviceId, tool.id);
    ElMessage.success(t("common.deleteSuccess"));
    emit("refresh");
  } catch (e: any) {
    if (e !== "cancel") ElMessage.error(e?.message || t("common.operationFailed"));
  }
};

const handleDialogSaved = () => {
  dialogVisible.value = false;
  emit("refresh");
};
</script>

<template>
  <div class="flex flex-col gap-3">
    <div class="custom-toolbar flex items-center gap-3 px-3.5 py-2.5">
      <el-input
        v-model="searchQuery"
        :placeholder="t('mcpService.tool.searchPlaceholder')"
        clearable
        class="search-input"
        :prefix-icon="Search"
      />
      <span class="total">
        {{ t("mcpService.tool.totalCount", { count: props.tools.length }) }}
        <template v-if="searchQuery">，{{ t("mcpService.tool.matchCount", { count: filteredTools.length }) }}</template>
      </span>
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        {{ t("mcpService.tool.addCustom") }}
      </el-button>
    </div>

    <el-empty v-if="filteredTools.length === 0 && searchQuery" :description="t('mcpService.tool.emptySearch')" />
    <el-empty v-else-if="filteredTools.length === 0" :description="t('mcpService.tool.emptyCustom')" />

    <div v-else class="custom-list">
      <div
        v-for="tool in filteredTools"
        :key="tool.id"
        class="custom-card flex items-start gap-4 px-[18px] py-4"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="custom-title">{{ tool.title || tool.name }}</span>
            <code class="custom-name">{{ tool.name }}</code>
            <el-tag size="small" type="info">{{ t("mcpService.tool.type.PARAMETERIZED_SQL") }}</el-tag>
          </div>
          <p class="custom-desc">{{ tool.description }}</p>

          <div v-if="tool.config" class="flex items-center gap-1.5 mt-2 text-xs custom-meta-color">
            <span class="flex items-center gap-1">
              <el-icon><Coin /></el-icon>
              {{ dsNameMap[(tool.config as any).data_source_id] || (tool.config as any).data_source_id }}
            </span>
            <span class="meta-divider">|</span>
            <span class="flex items-center gap-1">
              {{ t("mcpService.tool.paramCount", { count: (tool.config as any).parameters?.length ?? 0 }) }}
            </span>

          </div>
        </div>

        <div class="flex items-center gap-3.5 shrink-0">
          <el-tooltip content="编辑" placement="top">
            <el-icon class="action-icon" @click="handleEdit(tool)"><Edit /></el-icon>
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-icon class="action-icon action-delete" @click="handleDelete(tool)"><Delete /></el-icon>
          </el-tooltip>
          <el-tooltip content="测试" placement="top">
            <el-icon class="action-icon" @click="openTest(tool)"><CaretRight /></el-icon>
          </el-tooltip>
          <el-switch
            :model-value="tool.enabled"
            @change="handleToggle(tool)"
          />
        </div>
      </div>
    </div>

    <CustomToolDialog
      v-model="dialogVisible"
      :service-id="props.serviceId"
      :tool="editingTool"
      @saved="handleDialogSaved"
    />

    <ToolTestDialog
      v-if="testingTool"
      v-model:visible="testVisible"
      :service-id="props.serviceId"
      :tool="testingTool!"
    />
  </div>
</template>

<style scoped>
.custom-toolbar {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}
.search-input { width: 240px; }
.total { font-size: 13px; color: var(--ep-text-color-secondary); flex: 1; }
.custom-list {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
  overflow: hidden;
}
.custom-card {
  border-bottom: 1px solid var(--ep-border-color-lighter);
}
.custom-card:last-child { border-bottom: none; }
.custom-title { font-size: 14px; font-weight: 600; color: var(--ep-text-color-primary); }
.custom-name {
  font-size: 12px;
  color: var(--ep-text-color-secondary);
  background: var(--ep-fill-color);
  padding: 1px 6px;
  border-radius: 4px;
}
.custom-desc { margin-top: 4px; font-size: 13px; color: var(--ep-text-color-secondary); }
.custom-meta-color { color: var(--ep-text-color-secondary); }
.meta-divider { color: var(--ep-border-color); }
.confirm-badge { color: var(--ep-color-warning); }
.action-icon {
  cursor: pointer;
  color: var(--ep-text-color-secondary);
  font-size: 17px;
  transition: color 0.15s;
}
.action-icon:hover { color: var(--ep-color-primary); }
.action-delete:hover { color: var(--ep-color-danger); }
</style>
