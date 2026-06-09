<route lang="yaml">
meta:
  activeMenu: /mcp-services
</route>

<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import {
  DataAnalysis,
  Delete,
  Document,
  DocumentCopy,
  Key,
  Monitor,
  Operation,
  Promotion,
  SwitchButton,
  Tools,
  VideoPlay,
} from "@element-plus/icons-vue";
import type { McpServiceVO } from "~/api/mcp-service";
import { getMcpService, updateMcpService } from "~/api/mcp-service";
import DataScopeTab from "~/components/mcp-service/DataScopeTab.vue";
import PromptsTab from "~/components/mcp-service/PromptsTab.vue";
import ToolsTab from "~/components/mcp-service/ToolsTab.vue";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const route = useRoute("/mcp-services/[id]/");

const serviceId = route.params.id;
const loading = ref(false);
const saving = ref(false);
const service = ref<McpServiceVO | null>(null);

const formData = ref({
  name: "",
  description: "",
});

const activeTab = ref("basic");

const tabs = [
  { key: "basic", label: t("mcpService.tab.basic"), icon: Document },
  { key: "scope", label: t("mcpService.tab.dataScope"), icon: DataAnalysis },
  { key: "tools", label: t("mcpService.tab.tools"), icon: Tools },
  { key: "prompts", label: t("mcpService.tab.prompts"), icon: Promotion },
  { key: "security", label: t("mcpService.tab.security"), icon: Key },
  { key: "debug", label: t("mcpService.tab.debug"), icon: Monitor },
  { key: "logs", label: t("mcpService.tab.logs"), icon: Operation },
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

const endpointUrl = computed(() => {
  if (!service.value?.endpoint_path) return "";
  return window.location.origin + service.value.endpoint_path;
});

const isDirty = computed(() =>
  !!service.value &&
  (formData.value.name !== service.value.name ||
    formData.value.description.trim() !== (service.value.description || "")),
);

const serviceMeta = computed(() => [
  {
    label: t("common.id"),
    value: service.value?.id || "",
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
    console.error("加载服务详情失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

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
    await loadService();
  } catch (error) {
    console.error("保存失败:", error);
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

const handlePublish = () => {
  ElMessage.info(t("mcpService.comingSoon"));
};

const handleDisable = () => {
  ElMessage.info(t("mcpService.comingSoon"));
};

const handleEnable = () => {
  ElMessage.info(t("mcpService.comingSoon"));
};

const handleDelete = () => {
  ElMessage.info(t("mcpService.comingSoon"));
};

onMounted(() => {
  loadService();
});
</script>

<template>
  <div v-loading="loading" class="mcp-detail-page">
    <div class="detail-header flex items-start justify-between gap-4">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/mcp-services' }">
          {{ t("mcpService.title") }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ service?.name || serviceId }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-actions flex items-center gap-3">
        <el-button
          v-if="service?.status === 'DRAFT'"
          type="primary"
          :icon="VideoPlay"
          @click="handlePublish"
        >
          {{ t("mcpService.publish") }}
        </el-button>
        <el-button
          v-if="service?.status === 'PUBLISHED'"
          type="warning"
          :icon="SwitchButton"
          @click="handleDisable"
        >
          {{ t("mcpService.disable") }}
        </el-button>
        <el-button
          v-if="service?.status === 'DISABLED'"
          type="success"
          :icon="VideoPlay"
          @click="handleEnable"
        >
          {{ t("mcpService.enable") }}
        </el-button>
        <el-button
          v-if="service?.status === 'DRAFT'"
          type="danger"
          plain
          :icon="Delete"
          @click="handleDelete"
        >
          {{ t("common.delete") }}
        </el-button>
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
            <el-icon><component :is="tab.icon" /></el-icon>
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
                <div class="endpoint-copy w-full">
                  <el-input :model-value="endpointUrl" readonly />
                  <el-tooltip :content="t('common.copy')" placement="top">
                    <el-button :icon="DocumentCopy" @click="handleCopyEndpoint" />
                  </el-tooltip>
                </div>
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
          <DataScopeTab :service-id="serviceId" :service-status="service?.status" />
        </div>
        <div v-else-if="activeTab === 'tools'" class="scope-panel p-[18px]">
          <ToolsTab :service-id="serviceId" @refresh="loadService" />
        </div>
        <div v-else-if="activeTab === 'prompts'" class="scope-panel p-[18px]">
          <PromptsTab :service-id="serviceId" />
        </div>
        <div v-else class="coming-soon flex items-center justify-center min-h-[420px]">
          <el-empty :description="t('mcpService.comingSoon')">
            <el-button @click="activeTab = 'basic'">
              {{ t("mcpService.backToBasic") }}
            </el-button>
          </el-empty>
        </div>
      </main>
    </div>
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
.scope-panel,
.coming-soon {
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
