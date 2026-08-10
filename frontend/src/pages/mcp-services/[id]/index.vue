<route lang="yaml">
meta:
  activeMenu: /mcp-services
</route>

<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import {
  Clock,
  DataAnalysis,
  Document,
  DocumentCopy,
  SwitchButton,
  VideoPlay,
} from "@element-plus/icons-vue";
import type { McpServiceDiffVO, McpServiceVO } from "~/api/mcp-service";
import {
  disableMcpService,
  enableMcpService,
  getMcpService,
  getMcpServiceDiff,
  publishMcpService,
  updateMcpService,
} from "~/api/mcp-service";
import DataScopeTab from "~/components/mcp-service/DataScopeTab.vue";
import DebugPublishTab from "~/components/mcp-service/DebugPublishTab.vue";
import DiffSummaryList from "~/components/mcp-service/DiffSummaryList.vue";
import PromptsTab from "~/components/mcp-service/PromptsTab.vue";
import ToolsTab from "~/components/mcp-service/ToolsTab.vue";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const route = useRoute("/mcp-services/[id]/");

const serviceId = route.params.id;
const loading = ref(false);
const saving = ref(false);
const service = ref<McpServiceVO | null>(null);
const diff = ref<McpServiceDiffVO | null>(null);
const publishing = ref(false);
const statusChanging = ref(false);
const publishDialogVisible = ref(false);
const releaseNote = ref("");

const formData = ref({
  name: "",
  description: "",
});

const activeTab = ref("basic");

const tabs = [
  { key: "basic", label: t("mcpService.tab.basic"), icon: Document },
  { key: "scope", label: t("mcpService.tab.dataScope"), icon: DataAnalysis },
  { key: "tools", label: t("mcpService.tab.tools"), iconClass: "icon-[codicon--developer-tools]" },
  { key: "prompts", label: t("mcpService.tab.prompts"), iconClass: "icon-[fluent--prompt-16-regular]" },
  { key: "version", label: t("mcpService.tab.version"), icon: Clock },
];

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

const endpointUrl = computed(() => service.value?.endpoint_path || "");

const isDirty = computed(() =>
  !!service.value &&
  (formData.value.name !== service.value.name ||
    formData.value.description.trim() !== (service.value.description || "")),
);

/** 变更摘要（右上角 popover + 发布弹窗共用） */
const diffSummary = computed(() => {
  const d = diff.value;
  if (!d) return [];
  const items: {
    label: string;
    added?: string[];
    modified?: string[];
    deleted?: string[];
    detail?: string;
  }[] = [];

  if (d.basic_info_changed) {
    items.push({ label: t("mcpService.tab.basic"), detail: t("common.modified") });
  }
  if (d.data_scope_changed) {
    items.push({ label: t("mcpService.tab.dataScope"), detail: t("common.modified") });
  }
  if (d.tools_changed) {
    items.push({
      label: t("mcpService.tab.tools"),
      added: d.added_tools || [],
      modified: d.modified_tools || [],
      deleted: d.deleted_tools || [],
    });
  }
  if (d.prompts_changed) {
    items.push({
      label: t("mcpService.tab.prompts"),
      added: d.added_prompts || [],
      modified: d.modified_prompts || [],
      deleted: d.deleted_prompts || [],
    });
  }
  return items;
});

const serviceMeta = computed(() => [
  {
    label: t("common.id"),
    value: service.value?.id || "",
    copyable: true,
  },
  {
    label: t("mcpService.serviceCode"),
    value: service.value?.code || t("mcpService.emptyValue"),
    copyable: true,
  },
  {
    label: t("mcpService.toolCount"),
    value: service.value?.tool_count ?? 0,
  },
  {
    label: t("mcpService.endpointPath"),
    value: service.value?.endpoint_path || t("mcpService.notPublished"),
    copyable: true,
  },
  {
    label: t("common.updatedAt"),
    value: service.value?.updated_at
      ? formatDateTime(service.value.updated_at)
      : t("mcpService.emptyValue"),
  },
]);

const loadService = async () => {
  try {
    loading.value = true;
    const data = await getMcpService(serviceId);
    service.value = data;
    formData.value.name = data.name;
    formData.value.description = data.description || "";
  } catch (error) {
    console.error("Failed to load service detail:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const loadDiff = async () => {
  try {
    diff.value = await getMcpServiceDiff(serviceId);
  } catch (error) {
    console.error("Failed to load service diff:", error);
  }
};

const refreshAll = async () => {
  await Promise.all([loadService(), loadDiff()]);
};

// ── 发布 / 停用 / 启用（页面级状态操作）──

const openPublishDialog = () => {
  releaseNote.value = "";
  publishDialogVisible.value = true;
};

const confirmPublish = async () => {
  try {
    publishing.value = true;
    await publishMcpService(serviceId, { release_note: releaseNote.value.trim() });
    ElMessage.success(
      service.value?.status === "DISABLED"
        ? t("mcpService.publishSuccessDisabled")
        : t("mcpService.publishSuccess"),
    );
    publishDialogVisible.value = false;
    await refreshAll();
  } catch (error: any) {
    console.error("Failed to publish:", error);
    ElMessage.error(error?.message || t("common.operationFailed"));
  } finally {
    publishing.value = false;
  }
};

const handleDisable = async () => {
  try {
    await ElMessageBox.confirm(
      t("mcpService.disableConfirmMsg"),
      t("mcpService.disableConfirmTitle"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      },
    );
    statusChanging.value = true;
    await disableMcpService(serviceId);
    ElMessage.success(t("mcpService.disableSuccess"));
    await refreshAll();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("Failed to disable:", error);
      ElMessage.error(error?.message || t("common.operationFailed"));
    }
  } finally {
    statusChanging.value = false;
  }
};

const handleEnable = async () => {
  try {
    statusChanging.value = true;
    await enableMcpService(serviceId);
    ElMessage.success(t("mcpService.enableSuccess"));
    await refreshAll();
  } catch (error: any) {
    console.error("Failed to enable:", error);
    ElMessage.error(error?.message || t("common.operationFailed"));
  } finally {
    statusChanging.value = false;
  }
};

onMounted(() => {
  refreshAll();
});

const handleSave = async () => {
  if (!formData.value.name.trim()) {
    ElMessage.warning(t("common.required", { name: t("common.name") }));
    return;
  }
  try {
    saving.value = true;
    await updateMcpService(serviceId, {
      name: formData.value.name.trim(),
      description: formData.value.description.trim() || undefined,
    });
    ElMessage.success(t("common.saveSuccess"));
    await refreshAll();
  } catch (error) {
    console.error("Failed to save:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};

const handleCopy = async (text: string | number) => {
  const str = String(text);
  if (!str) return;
  try {
    await navigator.clipboard.writeText(str);
    ElMessage.success(t("mcpService.copySuccess"));
  } catch {
    ElMessage.error(t("mcpService.copyFailed"));
  }
};

const handleCopyEndpoint = async () => {
  await handleCopy(endpointUrl.value);
};
</script>

<template>
  <div v-loading="loading" class="mcp-detail-page">
    <div class="detail-header flex items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/mcp-services' }">
            {{ t("mcpService.title") }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ service?.name || serviceId }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-tag v-if="service" :type="statusType(service.status)" size="small" effect="plain">
          {{ statusLabel(service.status) }}
        </el-tag>
        <el-tag v-if="service?.active_version_number" type="primary" size="small" effect="light" class="font-mono">
          v{{ service.active_version_number }}
        </el-tag>
      </div>

      <div class="detail-actions flex items-center gap-3">
        <!-- DRAFT：首次发布 -->
        <el-button
          v-if="service?.status === 'DRAFT'"
          type="primary"
          :icon="VideoPlay"
          :loading="publishing"
          @click="openPublishDialog"
        >
          {{ t("mcpService.publish") }}
        </el-button>

        <!-- PUBLISHED：发布变更（有变更时）+ 停用 -->
        <template v-if="service?.status === 'PUBLISHED'">
          <el-popover
            v-if="diff?.has_changes"
            placement="bottom-end"
            :width="300"
            trigger="hover"
          >
            <template #reference>
              <el-button type="primary" :icon="VideoPlay" :loading="publishing" @click="openPublishDialog">
                {{ t("mcpService.publishChanges") }}
              </el-button>
            </template>
            <DiffSummaryList :items="diffSummary" :limit="5" :title="t('mcpService.hasUnpublishedChanges')" />
          </el-popover>
          <el-button
            type="warning"
            plain
            :icon="SwitchButton"
            :loading="statusChanging"
            @click="handleDisable"
          >
            {{ t("mcpService.disable") }}
          </el-button>
        </template>

        <!-- DISABLED：启用 + 发布变更（有变更时） -->
        <template v-if="service?.status === 'DISABLED'">
          <el-button
            type="success"
            :icon="VideoPlay"
            :loading="statusChanging"
            @click="handleEnable"
          >
            {{ t("mcpService.enable") }}
          </el-button>
          <el-button
            v-if="diff?.has_changes"
            type="primary"
            plain
            :icon="VideoPlay"
            :loading="publishing"
            @click="openPublishDialog"
          >
            {{ t("mcpService.publishChanges") }}
          </el-button>
        </template>
      </div>
    </div>

    <div class="detail-layout">
      <aside class="detail-nav">
        <el-menu
          :default-active="activeTab"
          class="detail-menu border-r-0"
          @select="(key: string) => (activeTab = key)"
        >
          <el-menu-item v-for="tab in tabs" :key="tab.key" :index="tab.key">
            <el-icon>
              <span v-if="tab.iconClass" :class="tab.iconClass"></span>
              <component v-else :is="tab.icon" />
            </el-icon>
            <span> {{ tab.label }} </span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="detail-main min-w-0">
        <div v-if="activeTab === 'basic'" class="basic-grid">
          <section class="panel min-w-0 p-[18px]">
            <div class="panel-heading flex items-start justify-between gap-3 mb-[18px]">
              <div>
                <h2>{{ t("mcpService.tab.basic") }}</h2>
                <span>{{ t("mcpService.basicSubtitle") }}</span>
              </div>
              <el-button type="primary" :loading="saving" :disabled="!isDirty" @click="handleSave">
                {{ t("common.save") }}
              </el-button>
            </div>

            <el-form label-position="top" class="detail-form">
              <el-form-item :label="t('common.name')" required>
                <el-input
                  v-model="formData.name"
                  :placeholder="t('common.placeholder.name')"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item :label="t('common.description')">
                <el-input
                  v-model="formData.description"
                  type="textarea"
                  :rows="5"
                  maxlength="500"
                  show-word-limit
                  :placeholder="t('common.placeholder.description')"
                />
              </el-form-item>
              <el-form-item
                v-if="service?.status === 'PUBLISHED'"
                :label="t('mcpService.endpointUrl')"
              >
                <el-input :model-value="endpointUrl" readonly class="font-mono">
                  <template #append>
                    <el-tooltip :content="t('common.copy')" placement="top">
                      <el-button :icon="DocumentCopy" @click="handleCopyEndpoint" />
                    </el-tooltip>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
          </section>

          <aside class="panel min-w-0 p-[18px]">
            <div class="panel-heading compact flex items-center justify-between gap-3 mb-[18px]">
              <div>
                <h2>{{ t("mcpService.overview") }}</h2>
                <span>{{ t("mcpService.status.label") }}</span>
              </div>
              <el-tag v-if="service" :type="statusType(service.status)" effect="plain">
                {{ statusLabel(service.status) }}
              </el-tag>
            </div>

            <div class="meta-list flex flex-col gap-3">
              <div v-for="item in serviceMeta" :key="item.label" class="meta-item flex gap-3 px-3 py-2.5">
                <div class="meta-content flex flex-col gap-0.5 flex-1 min-w-0">
                  <span>{{ item.label }}</span>
                  <div class="meta-value flex items-center gap-2">
                    <strong class="truncate" :title="String(item.value)">{{ item.value }}</strong>
                    <el-button
                      v-if="item.copyable"
                      link
                      :icon="DocumentCopy"
                      @click="handleCopy(item.value)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <div v-else-if="activeTab === 'scope'" class="scope-panel p-[18px]">
          <DataScopeTab :service-id="serviceId" :service-status="service?.status" @refresh="refreshAll" />
        </div>
        <div v-else-if="activeTab === 'tools'" class="scope-panel p-[18px]">
          <ToolsTab :service-id="serviceId" @refresh="refreshAll" />
        </div>
        <div v-else-if="activeTab === 'prompts'" class="scope-panel p-[18px]">
          <PromptsTab :service-id="serviceId" @refresh="refreshAll" />
        </div>
        <div v-else-if="activeTab === 'version'" class="scope-panel p-[18px]">
          <DebugPublishTab
            :service-id="serviceId"
            :service="service"
            @refresh="refreshAll"
          />
        </div>
      </main>
    </div>

    <!-- Publish Confirmation Dialog -->
    <el-dialog
      v-model="publishDialogVisible"
      :title="service?.status === 'PUBLISHED' ? t('mcpService.publishChangesConfirmTitle') : t('mcpService.publishConfirmTitle')"
      width="520px"
      append-to-body
    >
      <div class="flex flex-col gap-4">
        <p class="text-sm text-[var(--ep-text-color-regular)] m-0">
          {{ service?.status === 'PUBLISHED'
              ? t('mcpService.publishChangesConfirmDesc')
              : service?.status === 'DISABLED'
                ? t('mcpService.publishDisabledConfirmDesc')
                : t('mcpService.publishConfirmDesc') }}
        </p>

        <!-- 将发布内容摘要 -->
        <div
          v-if="diffSummary.length"
          class="rounded-lg p-3 bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)] flex flex-col gap-2"
        >
          <div class="text-xs font-semibold text-[var(--ep-text-color-primary)] mb-0.5 border-b border-[var(--ep-border-color-lighter)] pb-1.5">
            {{ t("mcpService.publishSummaryTitle") }}
          </div>
          <DiffSummaryList :items="diffSummary" />
        </div>

        <el-form label-position="top">
          <el-form-item :label="t('mcpService.releaseNote')">
            <el-input
              v-model="releaseNote"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              :placeholder="t('mcpService.releaseNotePlaceholder')"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer flex justify-end gap-3">
          <el-button @click="publishDialogVisible = false">{{ t("common.cancel") }}</el-button>
          <el-button type="primary" :loading="publishing" @click="confirmPublish">
            {{ t("mcpService.confirmPublish") }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>


<style scoped>
.mcp-detail-page {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 16px;
  padding: 24px; /* p-6 */
}

.detail-layout {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr);
  gap: 16px;
}

.detail-nav,
.panel,
.scope-panel {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}

.detail-nav {
  align-self: start;
  overflow: hidden;
}

.detail-menu :deep(.el-menu-item) {
  height: 44px;
}

.basic-grid {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(260px, 400px);
  gap: 16px;
}

.panel-heading h2 {
  margin: 0;
  color: var(--ep-text-color-primary);
  font-size: 16px;
  font-weight: 650;
}

.panel-heading span {
  color: var(--ep-text-color-secondary);
  font-size: 12px;
}

.detail-form :deep(.el-form-item__label) {
  color: var(--ep-text-color-primary);
  font-weight: 600;
}

.meta-content > span {
  color: var(--ep-text-color-secondary);
  font-size: 12px;
}

.meta-value strong {
  color: var(--ep-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.meta-item {
  border-radius: 6px;
  background: var(--ep-fill-color-lighter);
}

@media (max-width: 1200px) {
  .basic-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .detail-header,
  .detail-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-layout,
  .basic-grid {
    grid-template-columns: 1fr;
  }

  .detail-nav {
    overflow-x: auto;
  }
}

@media (max-width: 640px) {
  .mcp-detail-page {
    padding: 16px; /* p-4 */
  }
}
</style>
