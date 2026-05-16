<script setup lang="ts">
import type { McpServiceVO } from "~/api/mcp-service";
import { formatDateTime } from "~/composables";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

interface Props {
  data: McpServiceVO[];
  loading?: boolean;
}

interface Emits {
  (e: "detail", service: McpServiceVO): void;
  (e: "edit", service: McpServiceVO): void;
  (e: "delete", service: McpServiceVO): void;
}

defineProps<Props>();
defineEmits<Emits>();

const statusType = (status: string) => {
  switch (status) {
    case "DRAFT":
      return "info";
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
</script>

<template>
  <el-table :data="data" :loading="loading" stripe style="width: 100%">
    <el-table-column
      prop="name"
      :label="t('common.name')"
      min-width="160"
      show-overflow-tooltip
    />
    <el-table-column
      prop="description"
      :label="t('common.description')"
      min-width="200"
      show-overflow-tooltip
    />
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
      min-width="80"
    />
    <el-table-column
      prop="endpoint_path"
      :label="t('mcpService.endpointPath')"
      min-width="180"
      show-overflow-tooltip
    />
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
    >
      <template #default="{ row }">
        <div class="flex items-center gap-2">
          <el-button type="primary" link @click="$emit('detail', row)">
            {{ t("common.detail") }}
          </el-button>
          <el-button type="primary" link @click="$emit('edit', row)">
            {{ t("common.edit") }}
          </el-button>
          <el-button type="danger" link @click="$emit('delete', row)">
            {{ t("common.delete") }}
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>
