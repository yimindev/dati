<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { RefreshRight } from "@element-plus/icons-vue";
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
  <div v-loading="loading" class="debug-publish-tab flex flex-col gap-4">
    <!-- Snapshot History Table -->
    <el-table
      :data="snapshots"
      size="default"
      class="w-full"
      :empty-text="t('mcpService.emptyVersionHistory')"
    >
      <el-table-column prop="version_number" :label="t('mcpService.versionNumber')" width="120">
        <template #default="{ row }">
          <div class="flex items-center gap-2 font-mono font-semibold text-xs">
            <span>v{{ row.version_number }}</span>
            <span
              v-if="row.version_number === service?.active_version_number"
              class="text-[10px] px-1.5 py-0.2 rounded bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-medium"
            >
              {{ t("mcpService.activeTag") }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="release_note" :label="t('mcpService.releaseNote')">
        <template #default="{ row }">
          <span class="text-xs text-[var(--ep-text-color-primary)]">{{ row.release_note || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="created_at" :label="t('common.updatedAt')" width="180">
        <template #default="{ row }">
          <span class="text-xs text-[var(--ep-text-color-secondary)]">{{ row.created_at ? formatDateTime(row.created_at) : '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column :label="t('common.actions')" width="100" align="right">
        <template #default="{ row }">
          <el-button
            v-if="row.version_number !== service?.active_version_number"
            type="primary"
            link
            :icon="RefreshRight"
            :loading="rollingBack"
            @click="handleRollback(row)"
          >
            {{ t("common.rollback") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
