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
  Connection,
  DataAnalysis,
  Delete,
  Document,
  DocumentCopy,
  Edit,
  FolderOpened,
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
  { key: "resources", label: t("mcpService.tab.resources"), icon: FolderOpened },
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

const serviceMeta = computed(() => [
  {
    label: t("common.id"),
    value: service.value?.id || "",
    icon: Document,
    copyable: true,
  },
  {
    label: t("mcpService.toolCount"),
    value: service.value?.tool_count ?? 0,
    icon: Tools,
  },
  {
    label: t("mcpService.endpointPath"),
    value: service.value?.endpoint_path || t("mcpService.notPublished"),
    icon: Connection,
  },
  {
    label: t("common.updatedAt"),
    value: service.value?.updated_at
      ? formatDateTime(service.value.updated_at)
      : t("mcpService.emptyValue"),
    icon: Edit,
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
    <div class="detail-header">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/mcp-services' }">
          {{ t("mcpService.title") }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ service?.name || serviceId }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-actions">
        <el-button
          v-if="service?.status === 'DRAFT'"
          type="success"
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
          class="detail-menu"
          @select="(key: string) => (activeTab = key)"
        >
          <el-menu-item v-for="tab in tabs" :key="tab.key" :index="tab.key">
            <el-icon><component :is="tab.icon" /></el-icon>
            <span> {{ tab.label }} </span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="detail-main">
        <div v-if="activeTab === 'basic'" class="basic-grid">
          <section class="panel">
            <div class="panel-heading">
              <div>
                <h2>{{ t("mcpService.tab.basic") }}</h2>
                <span>{{ t("mcpService.basicSubtitle") }}</span>
              </div>
              <el-button type="primary" :loading="saving" @click="handleSave">
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
                <div class="endpoint-copy">
                  <el-input :model-value="endpointUrl" readonly />
                  <el-tooltip :content="t('common.copy')" placement="top">
                    <el-button :icon="DocumentCopy" @click="handleCopyEndpoint" />
                  </el-tooltip>
                </div>
              </el-form-item>
            </el-form>
          </section>

          <aside class="panel">
            <div class="panel-heading compact">
              <div>
                <h2>{{ t("mcpService.overview") }}</h2>
                <span>{{ t("mcpService.status.label") }}</span>
              </div>
              <el-tag v-if="service" :type="statusType(service.status)" effect="plain">
                {{ statusLabel(service.status) }}
              </el-tag>
            </div>

            <div class="meta-list">
              <div v-for="item in serviceMeta" :key="item.label" class="meta-item">
                <span class="meta-icon">
                  <el-icon><component :is="item.icon" /></el-icon>
                </span>
                <div class="meta-content">
                  <span>{{ item.label }}</span>
                  <div class="meta-value">
                    <strong :title="String(item.value)">{{ item.value }}</strong>
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

        <div v-else class="coming-soon">
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
  padding: 20px 24px 24px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-header,
.detail-actions,
.endpoint-copy {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-layout {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
}

.detail-nav,
.panel {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}

.detail-nav {
  align-self: start;
  overflow: hidden;
}

.detail-menu {
  border-right: 0;
}

.detail-menu :deep(.el-menu-item) {
  height: 44px;
}

.detail-main {
  min-width: 0;
}

.basic-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.panel {
  min-width: 0;
  padding: 18px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-heading.compact {
  align-items: center;
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

.detail-form {
  max-width: 720px;
}

.detail-form :deep(.el-form-item__label) {
  color: var(--ep-text-color-primary);
  font-weight: 600;
}

.endpoint-copy {
  width: 100%;
}

.meta-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--ep-fill-color-lighter);
}

.meta-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--ep-bg-color);
  color: var(--ep-color-primary);
}

.meta-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.meta-content > span {
  color: var(--ep-text-color-secondary);
  font-size: 12px;
}

.meta-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-value strong {
  overflow: hidden;
  color: var(--ep-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coming-soon {
  display: flex;
  min-height: 420px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
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
    padding: 16px;
  }


}
</style>
