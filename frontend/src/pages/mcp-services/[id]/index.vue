<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { DocumentCopy } from "@element-plus/icons-vue";
import type { McpServiceVO } from "~/api/mcp-service";
import { getMcpService, updateMcpService } from "~/api/mcp-service";

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
  { key: "basic", label: t("mcpService.tab.basic") },
  { key: "scope", label: t("mcpService.tab.dataScope") },
  { key: "tools", label: t("mcpService.tab.tools") },
  { key: "resources", label: t("mcpService.tab.resources") },
  { key: "prompts", label: t("mcpService.tab.prompts") },
  { key: "security", label: t("mcpService.tab.security") },
  { key: "debug", label: t("mcpService.tab.debug") },
  { key: "logs", label: t("mcpService.tab.logs") },
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

const handleCopyEndpoint = async () => {
  try {
    await navigator.clipboard.writeText(endpointUrl.value);
    ElMessage.success(t("mcpService.copySuccess"));
  } catch {
    ElMessage.error(t("mcpService.copyFailed"));
  }
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
  <div v-loading="loading" class="flex flex-col h-full">
    <!-- 面包屑 -->
    <el-breadcrumb separator="/" class="px-6 pt-4 pb-2">
      <el-breadcrumb-item :to="{ path: '/mcp-services' }">
        {{ t("mcpService.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ service?.name || "" }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="flex flex-1 overflow-hidden">
      <!-- 左侧导航 -->
      <div
        class="w-48 border-r border-[var(--ep-border-color)] p-2 flex-shrink-0"
      >
      <el-menu
        :default-active="activeTab"
        class="!border-r-0"
        @select="(key: string) => (activeTab = key)"
      >
        <el-menu-item
          v-for="tab in tabs"
          :key="tab.key"
          :index="tab.key"
        >
          {{ tab.label }}
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 右侧内容 -->
    <div class="flex-1 p-6 overflow-auto">
      <!-- 基础信息 -->
      <div v-if="activeTab === 'basic'">
        <h2 class="text-lg font-semibold mb-4">
          {{ t("mcpService.tab.basic") }}
        </h2>

        <el-form label-width="100px" class="max-w-lg">
          <el-form-item :label="t('common.name')">
            <el-input
              v-model="formData.name"
              :placeholder="t('common.placeholder.name')"
            />
          </el-form-item>
          <el-form-item :label="t('common.description')">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              :placeholder="t('common.placeholder.description')"
            />
          </el-form-item>
          <el-form-item :label="t('mcpService.status.label')">
            <el-tag :type="statusType(service?.status || '')">
              {{ statusLabel(service?.status || "") }}
            </el-tag>
          </el-form-item>
          <el-form-item
            v-if="service?.status === 'PUBLISHED'"
            :label="t('mcpService.endpointUrl')"
          >
            <div class="flex items-center gap-2">
              <el-input
                :model-value="endpointUrl"
                readonly
                class="!w-96"
              />
              <el-button :icon="DocumentCopy" @click="handleCopyEndpoint">
                {{ t("common.copy") }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">
              {{ t("common.save") }}
            </el-button>
            <el-button
              v-if="service?.status === 'DRAFT'"
              type="success"
              @click="handlePublish"
            >
              {{ t("mcpService.publish") }}
            </el-button>
            <el-button
              v-if="service?.status === 'PUBLISHED'"
              type="warning"
              @click="handleDisable"
            >
              {{ t("mcpService.disable") }}
            </el-button>
            <el-button
              v-if="service?.status === 'DISABLED'"
              type="success"
              @click="handleEnable"
            >
              {{ t("mcpService.enable") }}
            </el-button>
            <el-button
              v-if="service?.status === 'DRAFT'"
              type="danger"
              @click="handleDelete"
            >
              {{ t("common.delete") }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 其他 Tab 占位 -->
      <div v-else class="text-[var(--ep-text-color-secondary)] text-center mt-20">
        {{ t("mcpService.comingSoon") }}
      </div>
      </div>
    </div>
  </div>
</template>
