<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Setting, CaretRight } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import type { McpToolVO } from "~/api/mcp-tool";
import { updateTool } from "~/api/mcp-tool";
import ExecuteSqlConfigDialog from "./ExecuteSqlConfigDialog.vue";
import ToolTestDialog from "./ToolTestDialog.vue";

const { t } = useI18n();

const props = defineProps<{ tools: McpToolVO[]; serviceId: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();

const sqlConfigVisible = ref(false);
const testVisible = ref(false);
const testingTool = ref<McpToolVO | null>(null);
const openTest = (tool: McpToolVO) => {
  testingTool.value = tool;
  testVisible.value = true;
};

const handleToggle = async (tool: McpToolVO) => {
  try {
    await updateTool(props.serviceId, tool.id, { tool_type: tool.tool_type, enabled: !tool.enabled });
    tool.enabled = !tool.enabled;
    ElMessage.success(t("common.saveSuccess"));
    emit("refresh");
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  }
};

const handleSqlConfigSaved = () => {
  sqlConfigVisible.value = false;
  emit("refresh");
};

const toolTypeLabel = (status: string) => {
  switch (status) {
    case "SEARCH_METADATA": return t("mcpService.tool.type.SEARCH_METADATA");
    case "GET_TABLE_INFO": return t("mcpService.tool.type.GET_TABLE_INFO");
    case "LIST_TABLES": return t("mcpService.tool.type.LIST_TABLES");
    case "EXECUTE_SQL": return t("mcpService.tool.type.EXECUTE_SQL");
    case "UPDATE_TABLE_INFO": return t("mcpService.tool.type.UPDATE_TABLE_INFO");
    case "UPDATE_COLUMN_INFO": return t("mcpService.tool.type.UPDATE_COLUMN_INFO");
    case "UPSERT_TERM": return t("mcpService.tool.type.UPSERT_TERM");
    default: return status;
  }
};
</script>

<template>
  <div class="prebuilt-section">
    <div
      v-for="tool in tools"
      :key="tool.id"
      class="prebuilt-card flex items-start gap-4 px-[18px] py-4"
    >
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-1">
          <span class="card-title">{{ toolTypeLabel(tool.tool_type) }}</span>
          <code class="card-name">{{ tool.name }}</code>
        </div>
        <p class="card-desc">{{ tool.description }}</p>

        <div v-if="tool.tool_type === 'EXECUTE_SQL' && tool.config" class="card-meta flex items-center gap-1.5 mt-2 text-xs">
          <span class="meta-text">
            {{ t("mcpService.tool.allowedOps") }}:
            <el-tag
              v-for="op in Object.entries((tool.config as any).sql_policy || {})"
              :key="op[0]"
              v-show="op[1]"
              size="small"
              class="perm-tag"
            >
              {{ op[0].toUpperCase().replace('ALLOW_', '') }}
            </el-tag>
          </span>
          <span class="meta-divider">|</span>
          <span class="meta-text">{{ t("mcpService.tool.maxRows") }}: {{ (tool.config as any).max_rows ?? 1000 }}</span>
          <span class="meta-divider">|</span>
          <span class="meta-text">{{ t("mcpService.tool.timeout") }}: {{ (tool.config as any).timeout ?? 30 }}s</span>
        </div>
      </div>

      <div class="flex items-center gap-3.5 shrink-0">
        <el-tooltip v-if="tool.tool_type === 'EXECUTE_SQL'" :content="t('common.config')" placement="top">
          <el-icon
            class="config-icon"
            :aria-label="t('common.config')"
            @click="sqlConfigVisible = true"
          ><Setting /></el-icon>
        </el-tooltip>
        <el-tooltip
          :content="tool.enabled ? t('common.test') : t('mcpService.tool.disabledTestHint')"
          placement="top"
        >
          <el-icon
            class="config-icon"
            :class="{ 'icon-disabled': !tool.enabled }"
            :aria-label="t('common.test')"
            @click="tool.enabled ? openTest(tool) : undefined"
          ><CaretRight /></el-icon>
        </el-tooltip>
        <el-switch
          :model-value="tool.enabled"
          @change="handleToggle(tool)"
        />
      </div>
    </div>

    <ExecuteSqlConfigDialog
      v-if="sqlConfigVisible"
      v-model="sqlConfigVisible"
      :service-id="props.serviceId"
      :tool="tools.find(item => item.tool_type === 'EXECUTE_SQL')!"
      @saved="handleSqlConfigSaved"
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
.prebuilt-section {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
  overflow: hidden;
}
.prebuilt-card {
  border-bottom: 1px solid var(--ep-border-color-lighter);
  transition: background 0.15s;
}
.prebuilt-card:last-child { border-bottom: none; }
.prebuilt-card:hover { background: var(--ep-fill-color-lighter); }
.card-title { font-size: 14px; font-weight: 600; color: var(--ep-text-color-primary); }
.card-name {
  font-size: 12px;
  color: var(--ep-text-color-secondary);
  background: var(--ep-fill-color);
  padding: 1px 6px;
  border-radius: 4px;
}
.card-desc { margin-top: 4px; font-size: 13px; color: var(--ep-text-color-secondary); line-height: 1.5; }
.meta-text { display: flex; align-items: center; gap: 4px; color: var(--ep-text-color-secondary); }
.meta-divider { color: var(--ep-border-color); }
.perm-tag {
  --ep-tag-bg-color: var(--ep-color-purple-light-9);
  --ep-tag-text-color: var(--ep-color-purple);
  --ep-tag-border-color: var(--ep-color-purple-light-7);
}
.config-icon {
  cursor: pointer;
  color: var(--ep-text-color-secondary);
  font-size: 18px;
  transition: color 0.15s;
}
.config-icon:hover { color: var(--ep-color-primary); }
.config-icon.icon-disabled {
  cursor: not-allowed;
  color: var(--ep-text-color-placeholder);
  opacity: 0.45;
}
.config-icon.icon-disabled:hover {
  color: var(--ep-text-color-placeholder);
}
</style>
