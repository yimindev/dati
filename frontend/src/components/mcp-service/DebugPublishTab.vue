<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  DocumentCopy,
  RefreshRight,
} from "@element-plus/icons-vue";
import type { McpServiceSnapshotVO, McpServiceVO } from "~/api/mcp-service";
import {
  getMcpServiceSnapshots,
  rollbackMcpService,
} from "~/api/mcp-service";
import { formatDateTime } from "~/composables";

const props = defineProps<{
  serviceId: string;
  service: McpServiceVO | null;
}>();

const emit = defineEmits<{
  (e: "refresh"): void;
}>();

const { t } = useI18n();

const loading = ref(false);
const rollingBack = ref(false);
const snapshots = ref<McpServiceSnapshotVO[]>([]);

const endpointUrl = computed(() => props.service?.endpoint_path || "");

const loadSnapshots = async () => {
  if (!props.serviceId) return;
  try {
    loading.value = true;
    snapshots.value = await getMcpServiceSnapshots(props.serviceId);
  } catch (error) {
    console.error("Failed to load version history:", error);
  } finally {
    loading.value = false;
  }
};

const handleCopyEndpoint = async () => {
  if (!endpointUrl.value) return;
  try {
    await navigator.clipboard.writeText(endpointUrl.value);
    ElMessage.success(t("mcpService.copySuccess"));
  } catch {
    ElMessage.error(t("mcpService.copyFailed"));
  }
};

const handleRollback = async (snapshot: McpServiceSnapshotVO) => {
  try {
    await ElMessageBox.confirm(
      t("mcpService.rollbackConfirmMsg", { version: snapshot.version_number }),
      t("mcpService.rollbackConfirmTitle"),
      {
        confirmButtonText: t("common.rollback"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      },
    );

    rollingBack.value = true;
    await rollbackMcpService(props.serviceId, {
      target_version_number: snapshot.version_number,
    });
    ElMessage.success(t("mcpService.rollbackSuccess"));
    emit("refresh");
    await loadSnapshots();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("Failed to rollback:", error);
      ElMessage.error(error?.message || t("common.operationFailed"));
    }
  } finally {
    rollingBack.value = false;
  }
};

onMounted(() => {
  loadSnapshots();
});

watch(
  () => props.serviceId,
  () => {
    loadSnapshots();
  },
);

// 回滚后 active_version_number 变化 → 重新加载版本历史
watch(
  () => props.service?.active_version_number,
  () => {
    loadSnapshots();
  },
);
</script>

<template>
  <div v-loading="loading" class="debug-publish-tab flex flex-col gap-5">
    <!-- Status & Endpoint Info Card -->
    <div class="panel p-5">
      <div class="flex flex-wrap items-center gap-3 mb-4">
        <h2 class="text-base font-semibold text-[var(--ep-text-color-primary)] m-0">
          {{ t("mcpService.tab.version") }}
        </h2>
        <el-tag v-if="service?.active_version_number" type="primary" effect="light" class="font-mono">
          v{{ service.active_version_number }}
        </el-tag>
      </div>

      <!-- Endpoint URL Input -->
      <div class="endpoint-section rounded-lg p-4 bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)]">
        <span class="text-xs font-semibold text-[var(--ep-text-color-secondary)] mb-2 block">
          {{ t("mcpService.endpointUrl") }}
        </span>
        <el-input :model-value="endpointUrl" readonly class="font-mono text-sm">
          <template #append>
            <el-tooltip :content="t('common.copy')" placement="top">
              <el-button :icon="DocumentCopy" @click="handleCopyEndpoint" />
            </el-tooltip>
          </template>
        </el-input>
      </div>
    </div>

    <!-- Snapshot History Card -->
    <div v-if="snapshots.length > 0" class="panel p-5 flex flex-col gap-4">
      <div class="flex items-center justify-between border-b border-[var(--ep-border-color-lighter)] pb-3">
        <h3 class="text-sm font-semibold text-[var(--ep-text-color-primary)] m-0">
          {{ t("mcpService.versionHistory") }}
        </h3>
      </div>

      <el-table :data="snapshots" size="small" class="w-full">
        <el-table-column prop="version_number" :label="t('mcpService.versionNumber')" width="100">
          <template #default="{ row }">
            <div class="flex items-center gap-1 font-mono font-medium">
              <span>v{{ row.version_number }}</span>
              <el-tag
                v-if="row.version_number === service?.active_version_number"
                type="success"
                size="small"
                effect="plain"
              >
                {{ t("mcpService.activeTag") }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="release_note" :label="t('mcpService.releaseNote')">
          <template #default="{ row }">
            <span>{{ row.release_note || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" :label="t('common.updatedAt')" width="180">
          <template #default="{ row }">
            <span>{{ row.created_at ? formatDateTime(row.created_at) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="100" align="right">
          <template #default="{ row }">
            <el-button
              v-if="row.version_number !== service?.active_version_number"
              type="primary"
              link
              :icon="RefreshRight"
              @click="handleRollback(row)"
            >
              {{ t("common.rollback") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.panel {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}
</style>
