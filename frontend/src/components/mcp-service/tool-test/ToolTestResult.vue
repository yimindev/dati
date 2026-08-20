<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { CircleCloseFilled } from "@element-plus/icons-vue";
import type {
  ToolTestResponse,
  SqlExecution,
  TableMetadata,
  SearchHit,
  MetadataUpdateData,
  TableListData,
} from "~/api/mcp-tool-test";

const props = defineProps<{
  loading: boolean;
  response: ToolTestResponse | null;
}>();

const { t } = useI18n();

// --- Error advice ---
const errorAdvice = computed(() => {
  const cat = props.response?.error?.error_category;
  switch (cat) {
    case "PARAM_ERROR":
      return t("mcpService.toolTest.adviceParamError");
    case "SCOPE_ERROR":
      return t("mcpService.toolTest.adviceScopeError");
    case "PERMISSION_DENIED":
      return t("mcpService.toolTest.advicePermissionDenied");
    case "SQL_ERROR":
      return t("mcpService.toolTest.adviceSqlError");
    case "TIMEOUT":
      return t("mcpService.toolTest.adviceTimeout");
    default:
      return "";
  }
});
</script>

<template>
  <div
    class="right-panel flex-1 pl-5 overflow-y-auto overflow-x-hidden relative min-w-0"
  >
    <!-- Initial empty state -->
    <div v-if="!loading && !response" class="result-empty">
      <el-empty :description="t('mcpService.toolTest.hint')" />
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="result-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- Result -->
    <template v-if="response">
      <div
        class="result-header flex justify-between items-center text-[13px] font-semibold text-[var(--ep-text-color-primary)] mb-3"
      >
        <span>{{ t("mcpService.toolTest.result") }}</span>
        <span class="text-xs font-normal text-[var(--ep-text-color-placeholder)]">{{
          t("mcpService.toolTest.elapsed", {
            ms: response.execution_time_ms,
          })
        }}</span>
      </div>

      <!-- Top-level error -->
      <div v-if="!response.success && response.error" class="error-card">
        <div class="error-header">
          <el-icon><CircleCloseFilled /></el-icon>
          <span class="error-category">{{
            response.error.error_category
          }}</span>
        </div>
        <pre class="error-message">{{ response.error.message }}</pre>
        <div v-if="errorAdvice" class="error-advice">{{ errorAdvice }}</div>
      </div>

      <SqlExecutionResult
        v-else-if="response.data?.type === 'SQL_EXECUTION'"
        :data="response.data as SqlExecution"
      />
      <TableMetadataResult
        v-else-if="response.data?.type === 'TABLE_METADATA'"
        :data="response.data as TableMetadata"
      />
      <SearchHitResult
        v-else-if="response.data?.type === 'SEARCH_HIT'"
        :data="response.data as SearchHit"
      />
      <MetadataUpdateResult
        v-else-if="response.data?.type === 'METADATA_UPDATE'"
        :data="response.data as MetadataUpdateData"
      />
      <TableListResult
        v-else-if="response.data?.type === 'TABLE_LIST'"
        :data="response.data as TableListData"
      />
    </template>
  </div>
</template>

<style scoped>
.error-card {
  border: 1px solid var(--ep-color-danger-light-7);
  border-left: 4px solid var(--ep-color-danger);
  border-radius: 0 8px 8px 0;
  padding: 16px;
  background: var(--ep-color-danger-light-9);
}
.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--ep-color-danger);
  margin-bottom: 10px;
}
.error-category {
  font-family: monospace;
  font-size: 12px;
}
.error-message {
  font-family: monospace;
  font-size: 12px;
  color: var(--ep-color-danger);
  background: var(--ep-bg-color);
  padding: 8px 12px;
  border-radius: 4px;
  margin: 0 0 10px;
  white-space: pre-wrap;
  word-break: break-word;
}
.error-advice {
  font-size: 13px;
  color: var(--ep-text-color-secondary);
}
</style>
