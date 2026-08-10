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
  DocumentCopy,
  InfoFilled,
  Link,
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
import { listPrompts } from "~/api/mcp-prompt";
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

const promptCount = ref(0);

const tabs = computed(() => [
  { key: "basic", label: t("mcpService.tab.basic") },
  { key: "scope", label: t("mcpService.tab.dataScope") },
  { key: "tools", label: t("mcpService.tab.tools"), badge: service.value?.tool_count ?? 0 },
  { key: "prompts", label: t("mcpService.tab.prompts"), badge: promptCount.value },
  { key: "version", label: t("mcpService.tab.version") },
]);

const statusLabel = (status?: string) => {
  switch (status) {
    case "DRAFT":
      return t("mcpService.status.draft");
    case "PUBLISHED":
      return t("mcpService.status.published");
    case "DISABLED":
      return t("mcpService.status.disabled");
    default:
      return status || "";
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

const loadPrompts = async () => {
  try {
    const list = await listPrompts(serviceId);
    promptCount.value = list.length;
  } catch (error) {
    console.error("Failed to load prompts count:", error);
  }
};

const refreshAll = async () => {
  await Promise.all([loadService(), loadDiff(), loadPrompts()]);
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
</script>

<template>
  <div v-loading="loading" class="mcp-detail-page flex flex-col gap-5 p-6">
    <!-- Top Navigation Breadcrumbs & Action Bar -->
    <div class="detail-header flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/mcp-services' }">
            {{ t("mcpService.title") }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ service?.name || serviceId }}</el-breadcrumb-item>
        </el-breadcrumb>

        <span
          v-if="service"
          class="status-dot-badge inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium transition-all"
          :class="{
            'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/50': service.status === 'PUBLISHED',
            'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-400 border border-amber-200 dark:border-amber-800/50': service.status === 'DISABLED',
            'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 border border-slate-200 dark:border-slate-700': service.status === 'DRAFT',
          }"
        >
          <span
            class="w-1.5 h-1.5 rounded-full"
            :class="{
              'bg-emerald-500 animate-pulse': service.status === 'PUBLISHED',
              'bg-amber-500': service.status === 'DISABLED',
              'bg-slate-400': service.status === 'DRAFT',
            }"
          />
          {{ statusLabel(service.status) }}
        </span>

        <span v-if="service?.active_version_number" class="text-xs font-mono text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color-light)] px-2 py-0.5 rounded-md border border-[var(--ep-border-color-lighter)]">
          v{{ service.active_version_number }}
        </span>
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

    <!-- Top Horizontal Underline Tabs -->
    <div class="border-b border-[var(--ep-border-color-lighter)]">
      <nav class="flex items-center gap-6 -mb-px overflow-x-auto">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="py-2.5 px-1 relative text-sm font-medium transition-colors flex items-center gap-2 cursor-pointer whitespace-nowrap border-b-2 bg-transparent"
          :class="activeTab === tab.key
            ? 'text-[var(--ep-color-primary)] border-[var(--ep-color-primary)] font-semibold'
            : 'text-[var(--ep-text-color-secondary)] border-transparent hover:text-[var(--ep-text-color-primary)] hover:border-[var(--ep-border-color)]'"
          @click="activeTab = tab.key"
        >
          <span>{{ tab.label }}</span>
          <span
            v-if="tab.badge !== undefined"
            class="text-[11px] px-2 py-0.5 rounded-full font-mono transition-colors font-semibold"
            :class="activeTab === tab.key
              ? 'bg-[var(--ep-color-primary-light-9)] text-[var(--ep-color-primary)]'
              : 'bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-secondary)]'"
          >
            {{ tab.badge }}
          </span>
        </button>
      </nav>
    </div>

    <!-- Tab Content Body -->
    <main class="detail-main min-w-0 flex-1">
      <!-- Full-Width Responsive Dual-Card Grid Layout (7:5 Split) -->
      <div v-if="activeTab === 'basic'" class="grid grid-cols-1 lg:grid-cols-12 gap-6 w-full">
        <!-- Left Card: Basic Settings Form (7 cols out of 12) -->
        <section class="panel p-6 lg:col-span-7 flex flex-col justify-between shadow-sm border border-[var(--ep-border-color-lighter)] rounded-xl bg-[var(--ep-bg-color)]">
          <el-form label-position="top" class="detail-form flex flex-col gap-4">
            <el-form-item :label="t('common.name')" required class="!mb-0">
              <el-input
                v-model="formData.name"
                :placeholder="t('common.placeholder.name')"
                maxlength="100"
                show-word-limit
              />
            </el-form-item>
            <el-form-item :label="t('common.description')" class="!mb-0">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="6"
                maxlength="500"
                show-word-limit
                :placeholder="t('common.placeholder.description')"
              />
            </el-form-item>
          </el-form>

          <div class="mt-6 pt-4 border-t border-[var(--ep-border-color-lighter)] flex items-center justify-between">
            <el-button type="primary" :loading="saving" :disabled="!isDirty" @click="handleSave">
              {{ t("common.save") }}
            </el-button>

            <div class="flex items-center gap-3 text-xs text-[var(--ep-text-color-secondary)]">
              <span v-if="isDirty" class="text-amber-500 font-medium flex items-center gap-1">
                <el-icon><InfoFilled /></el-icon> {{ t("common.unsavedChanges") }}
              </span>
              <span>{{ t("common.lastModifiedAt", { time: service?.updated_at ? formatDateTime(service.updated_at) : '-' }) }}</span>
            </div>
          </div>
        </section>

        <!-- Right Card: Technical Access & Metadata Card (5 cols out of 12) -->
        <aside class="panel p-6 lg:col-span-5 flex flex-col gap-5 shadow-sm border border-[var(--ep-border-color-lighter)] rounded-xl bg-[var(--ep-bg-color)]">

          <!-- MCP Endpoint Box -->
          <div class="endpoint-card p-4 rounded-lg bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)] flex flex-col gap-2.5">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-1.5">
                <el-icon class="text-[var(--ep-color-primary)]"><Link /></el-icon> {{ t("mcpService.endpointPath") }}
              </span>
              <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-[var(--ep-color-primary-light-9)] text-[var(--ep-color-primary)] font-medium">
                Streamable HTTP
              </span>
            </div>

            <div class="flex items-center justify-between gap-2 p-2.5 rounded-md bg-[var(--ep-bg-color)] border border-[var(--ep-border-color-lighter)] min-w-0">
              <span class="font-mono text-xs text-[var(--ep-text-color-primary)] truncate" :title="endpointUrl || t('mcpService.notPublished')">
                {{ endpointUrl || t('mcpService.notPublished') }}
              </span>
              <el-tooltip :content="t('common.copy')" placement="top">
                <el-button
                  v-if="endpointUrl"
                  link
                  :icon="DocumentCopy"
                  class="!p-1 text-[var(--ep-color-primary)] hover:opacity-80"
                  @click="handleCopy(endpointUrl)"
                />
              </el-tooltip>
            </div>
          </div>

          <!-- Technical Attributes List -->
          <div class="tech-attributes flex flex-col gap-3">
            <div class="attr-row flex items-center justify-between p-3 rounded-lg bg-[var(--ep-fill-color-lighter)] text-xs">
              <span class="text-[var(--ep-text-color-secondary)]">MCP 协议版本</span>
              <span class="font-mono font-medium text-[var(--ep-text-color-primary)]">2025-11-25</span>
            </div>

            <div class="attr-row flex items-center justify-between p-3 rounded-lg bg-[var(--ep-fill-color-lighter)] text-xs">
              <span class="text-[var(--ep-text-color-secondary)]">服务标识 (Code)</span>
              <div class="flex items-center gap-1.5">
                <span class="font-mono font-medium text-[var(--ep-text-color-primary)]">{{ service?.code || '-' }}</span>
                <el-button v-if="service?.code" link :icon="DocumentCopy" class="!p-0 !h-auto text-[var(--ep-text-color-secondary)] hover:text-[var(--ep-color-primary)]" @click="handleCopy(service.code)" />
              </div>
            </div>

            <div class="attr-row flex items-center justify-between p-3 rounded-lg bg-[var(--ep-fill-color-lighter)] text-xs gap-2">
              <span class="text-[var(--ep-text-color-secondary)] shrink-0">ID</span>
              <div class="flex items-center gap-1.5 min-w-0">
                <span class="font-mono text-xs text-[var(--ep-text-color-primary)]" :title="service?.id">{{ service?.id || '-' }}</span>
                <el-button v-if="service?.id" link :icon="DocumentCopy" class="!p-0 !h-auto text-[var(--ep-text-color-secondary)] hover:text-[var(--ep-color-primary)] shrink-0" @click="handleCopy(service.id)" />
              </div>
            </div>
          </div>
        </aside>
      </div>

      <div v-else-if="activeTab === 'scope'" class="scope-panel p-[20px] shadow-sm">
        <DataScopeTab :service-id="serviceId" @refresh="refreshAll" />
      </div>
      <div v-else-if="activeTab === 'tools'" class="scope-panel p-[20px] shadow-sm">
        <ToolsTab :service-id="serviceId" @refresh="refreshAll" />
      </div>
      <div v-else-if="activeTab === 'prompts'" class="scope-panel p-[20px] shadow-sm">
        <PromptsTab :service-id="serviceId" @refresh="refreshAll" />
      </div>
      <div v-else-if="activeTab === 'version'" class="scope-panel p-[20px] shadow-sm">
        <DebugPublishTab
          :service-id="serviceId"
          :service="service"
          @refresh="refreshAll"
        />
      </div>
    </main>

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
.panel,
.scope-panel {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
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
</style>
