<script setup lang="ts">
import { ElMessage } from "element-plus";
import { DocumentCopy, MoreFilled } from "@element-plus/icons-vue";
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

const statusClass = (status: string) => {
  switch (status) {
    case "PUBLISHED":
      return "status-dot published";
    case "DISABLED":
      return "status-dot disabled";
    default:
      return "status-dot draft";
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
    :loading="loading"
    row-key="id"
    class="mcp-service-table"
    style="width: 100%"
  >
    <el-table-column
      prop="name"
      :label="t('mcpService.serviceName')"
      min-width="220"
      fixed="left"
    >
      <template #default="{ row }">
        <button class="service-cell" type="button" @click="$emit('detail', row)">
          <span class="service-main">
            <span class="service-name">{{ row.name }}</span>
            <span class="service-id">{{ row.id }}</span>
          </span>
        </button>
      </template>
    </el-table-column>
    <el-table-column
      prop="description"
      :label="t('common.description')"
      min-width="240"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        <span class="description-text">
          {{ row.description || t("mcpService.emptyDescription") }}
        </span>
      </template>
    </el-table-column>
    <el-table-column
      prop="status"
      :label="t('mcpService.status.label')"
      min-width="120"
    >
      <template #default="{ row }">
        <span class="status-cell">
          <span :class="statusClass(row.status)"></span>
          <el-tag :type="statusType(row.status)" size="small" effect="plain">
            {{ statusLabel(row.status) }}
          </el-tag>
        </span>
      </template>
    </el-table-column>
    <el-table-column
      prop="tool_count"
      :label="t('mcpService.toolCount')"
      min-width="100"
      align="right"
    >
      <template #default="{ row }">
        <el-tag round effect="plain">{{ row.tool_count ?? 0 }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column
      prop="endpoint_path"
      :label="t('mcpService.endpointPath')"
      min-width="260"
    >
      <template #default="{ row }">
        <div v-if="row.endpoint_path" class="endpoint-cell">
          <el-tooltip :content="row.endpoint_path" placement="top">
            <code>{{ row.endpoint_path }}</code>
          </el-tooltip>
          <el-tooltip :content="t('common.copy')" placement="top">
            <el-button
              link
              :icon="DocumentCopy"
              @click="handleCopy(row.endpoint_path)"
            />
          </el-tooltip>
        </div>
        <span v-else class="description-text">{{ t("mcpService.notPublished") }}</span>
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
      width="190"
      fixed="right"
      align="right"
    >
      <template #default="{ row }">
        <div class="action-cell">
          <el-button type="primary" link @click="$emit('detail', row)">
            {{ t("common.detail") }}
          </el-button>
          <el-button type="primary" link @click="$emit('edit', row)">
            {{ t("common.edit") }}
          </el-button>
          <el-dropdown trigger="click">
            <el-button link :icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$emit('delete', row)">
                  <span class="danger-action">{{ t("common.delete") }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty :description="t('mcpService.emptyList')" />
    </template>
  </el-table>
</template>

<style scoped>
.service-cell {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 0;
  text-align: left;
}

.service-cell:hover .service-name {
  color: var(--ep-color-primary);
}

.service-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.service-name {
  overflow: hidden;
  color: var(--ep-text-color-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-id,
.description-text {
  color: var(--ep-text-color-secondary);
  font-size: 12px;
}

.status-cell,
.endpoint-cell,
.action-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
}

.status-dot.draft {
  background: var(--ep-color-info);
}

.status-dot.published {
  background: var(--ep-color-success);
}

.status-dot.disabled {
  background: var(--ep-color-danger);
}

.endpoint-cell {
  max-width: 100%;
}

.endpoint-cell code {
  overflow: hidden;
  max-width: 210px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.danger-action {
  color: var(--ep-color-danger);
}
</style>
