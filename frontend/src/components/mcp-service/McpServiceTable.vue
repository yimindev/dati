<script setup lang="ts">
import { ElMessage } from "element-plus";
import { DocumentCopy } from "@element-plus/icons-vue";
import type { McpServiceVO } from "~/api/mcp-service";
import { formatDateTime } from "~/composables";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

interface Props {
  data: McpServiceVO[];
}

interface Emits {
  (e: "detail", service: McpServiceVO): void;
  (e: "delete", service: McpServiceVO): void;
}

defineProps<Props>();
defineEmits<Emits>();

const statusType = (status: string) => {
  switch (status) {
    case "PUBLISHED":
      return "success";
    case "DISABLED":
      return "danger";
    default:
      return "info";
  }
};

const statusLabel = (status: string) => {
  switch (status) {
    case "DRAFT":
      return t("mcpService.status.draft");
    case "PUBLISHED":
      return t("mcpService.status.published");
    case "DISABLED":
      return t("mcpService.status.disabled");
    default:
      return status;
  }
};

const handleCopy = async (endpointPath: string) => {
  try {
    await navigator.clipboard.writeText(endpointPath);
    ElMessage.success(t("mcpService.copySuccess"));
  } catch {
    ElMessage.error(t("mcpService.copyFailed"));
  }
};
</script>

<template>
  <el-table
    :data="data"
    stripe
    style="width: 100%"
  >
    <el-table-column
      prop="name"
      :label="t('mcpService.serviceName')"
      min-width="200"
    >
      <template #default="{ row }">
        <div class="flex flex-col gap-0.5 min-w-0">
          <el-button
            link
            type="primary"
            class="!justify-start font-medium text-left !p-0 truncate"
            @click="$emit('detail', row)"
          >
            {{ row.name }}
          </el-button>
          <span class="text-xs text-[var(--ep-text-color-placeholder)] font-mono truncate">
            {{ row.code }}
          </span>
        </div>
      </template>
    </el-table-column>
    <el-table-column
      prop="description"
      :label="t('common.description')"
      min-width="200"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        {{ row.description || '-' }}
      </template>
    </el-table-column>
    <el-table-column
      prop="status"
      :label="t('mcpService.status.label')"
      min-width="100"
    >
      <template #default="{ row }">
        <el-tag :type="statusType(row.status)" size="small">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column
      prop="tool_count"
      :label="t('mcpService.toolCount')"
      min-width="90"
      align="right"
    >
      <template #default="{ row }">
        <span class="font-mono text-sm">{{ row.tool_count ?? 0 }}</span>
      </template>
    </el-table-column>
    <el-table-column
      prop="endpoint_path"
      :label="t('mcpService.endpointPath')"
      min-width="240"
    >
      <template #default="{ row }">
        <div v-if="row.endpoint_path" class="flex items-center gap-1.5 min-w-0">
          <code class="text-xs px-1.5 py-0.5 rounded bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-primary)] font-mono truncate max-w-[180px]">
            {{ row.endpoint_path }}
          </code>
          <el-button
            link
            type="primary"
            :icon="DocumentCopy"
            :aria-label="t('common.copy')"
            @click="handleCopy(row.endpoint_path)"
          />
        </div>
        <span v-else class="text-sm text-[var(--ep-text-color-placeholder)]">-</span>
      </template>
    </el-table-column>
    <el-table-column
      prop="updated_at"
      :label="t('common.updatedAt')"
      min-width="160"
    >
      <template #default="{ row }">
        {{ formatDateTime(row.updated_at) }}
      </template>
    </el-table-column>
    <el-table-column
      :label="t('common.actions')"
      width="180"
      fixed="right"
      align="right"
    >
      <template #default="{ row }">
        <div class="flex items-center justify-end gap-2">
          <el-button type="primary" link @click="$emit('detail', row)">
            {{ t("common.detail") }}
          </el-button>
          <el-button type="danger" link @click="$emit('delete', row)">
            {{ t("common.delete") }}
          </el-button>
        </div>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty :description="t('mcpService.emptyList')" />
    </template>
  </el-table>
</template>

