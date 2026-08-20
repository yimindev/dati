<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import {
  DocumentCopy,
  InfoFilled,
  Right,
} from "@element-plus/icons-vue";
import type { McpServiceVO } from "~/api/mcp-service";

const props = defineProps<{
  service: McpServiceVO | null;
}>();

const { t } = useI18n();
const router = useRouter();

// ── 端点与 URL ──
const endpointUrl = computed(() => {
  if (!props.service?.endpoint_path) return "";
  const path = props.service.endpoint_path;
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  if (typeof window !== "undefined") {
    return `${window.location.origin}${path.startsWith("/") ? "" : "/"}${path}`;
  }
  return path;
});

// ── 通用 JSON 接入配置 ──
const jsonConfigSnippet = computed(() => {
  const code = props.service?.code || "my-mcp-service";
  const url = endpointUrl.value || `http://localhost:8085/${code}/mcp`;

  const configObj = {
    mcpServers: {
      [code]: {
        url,
        type: "streamable-http",
        headers: {
          Authorization: "Bearer <YOUR_API_KEY>",
        },
      },
    },
  };

  return JSON.stringify(configObj, null, 2);
});

const handleCopy = async (text: string, successMessage?: string) => {
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(successMessage || t("common.copySuccess"));
  } catch {
    ElMessage.error(t("common.copyFailed"));
  }
};

const goToApiKeys = () => {
  router.push("/settings/api-keys");
};
</script>

<template>
  <aside class="mcp-access-card panel p-6 flex flex-col justify-between shadow-sm border border-[var(--ep-border-color-lighter)] rounded-xl bg-[var(--ep-bg-color)]">
    <!-- Top Configuration Content Area -->
    <div class="flex flex-col gap-4">
      <!-- 1. 服务访问端点 (Endpoint Box) -->
      <div class="flex flex-col gap-2">
        <div class="flex items-center justify-between gap-2 flex-wrap">
          <label class="text-sm text-[var(--ep-text-color-regular)]">
            {{ t("mcpService.accessConfig.endpointTitle") }}
          </label>
          <div class="flex items-center gap-2 flex-wrap">
            <span class="text-[11px] font-mono px-2 py-0.5 rounded bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-secondary)] border border-[var(--ep-border-color-lighter)]">
              Streamable HTTP
            </span>
            <span class="text-[11px] font-mono px-2 py-0.5 rounded bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-secondary)] border border-[var(--ep-border-color-lighter)]">
              Version 2025-11-25
            </span>
            <span
              v-if="service?.status === 'DRAFT'"
              class="text-[11px] text-[var(--ep-color-warning)] flex items-center gap-1 font-medium"
            >
              <el-icon><InfoFilled /></el-icon>
              {{ t("mcpService.notPublished") }}
            </span>
          </div>
        </div>

        <div class="flex items-center justify-between gap-2 p-2.5 rounded-lg bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)] min-w-0">
          <span class="font-mono text-xs text-[var(--ep-text-color-primary)] truncate select-all" :title="endpointUrl">
            {{ endpointUrl || t("mcpService.notPublished") }}
          </span>
          <el-tooltip :content="t('common.copy')" placement="top">
            <el-button
              v-if="endpointUrl"
              link
              :icon="DocumentCopy"
              class="!p-1 text-[var(--ep-color-primary)] hover:opacity-80 shrink-0"
              @click="handleCopy(endpointUrl)"
            />
          </el-tooltip>
        </div>
      </div>

      <!-- 2. JSON 配置 -->
      <div class="flex flex-col gap-2">
        <div class="flex items-center gap-1.5">
          <span class="text-sm text-[var(--ep-text-color-regular)]">
            {{ t("mcpService.accessConfig.configTitle") }}
          </span>
          <el-tooltip :content="t('mcpService.accessConfig.configGuide')" placement="top">
            <el-icon class="text-[var(--ep-text-color-placeholder)] hover:text-[var(--ep-text-color-secondary)] cursor-help !text-sm transition-colors">
              <span class="icon-[codicon--question]" />
            </el-icon>
          </el-tooltip>
        </div>

        <!-- Code block container with embedded top-right copy button -->
        <div class="relative group">
          <pre class="m-0 font-mono text-xs text-[var(--ep-text-color-primary)] whitespace-pre-wrap break-all leading-relaxed select-all rounded-lg bg-[var(--ep-fill-color-light)] border border-[var(--ep-border-color-lighter)] p-3.5 pr-10">{{ jsonConfigSnippet }}</pre>
          <el-tooltip :content="t('common.copy')" placement="top">
            <el-button
              link
              :icon="DocumentCopy"
              class="!absolute top-2 right-2 !p-1.5 !h-auto text-[var(--ep-text-color-placeholder)] hover:text-[var(--ep-color-primary)] hover:bg-[var(--ep-bg-color)] rounded transition-all"
              @click="handleCopy(jsonConfigSnippet, t('mcpService.accessConfig.copySuccess'))"
            />
          </el-tooltip>
        </div>
      </div>
    </div>

    <!-- 3. 底部 API Key 获取入口（与左侧操作栏分割线平齐） -->
    <div class="mt-4 pt-4 border-t border-[var(--ep-border-color-lighter)] flex items-center">
      <el-button
        type="primary"
        link
        :icon="Right"
        class="!p-0 !h-auto text-xs"
        @click="goToApiKeys"
      >
        {{ t("mcpService.accessConfig.manageApiKeys") }}
      </el-button>
    </div>
  </aside>
</template>

<style scoped>
.mcp-access-card {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}
</style>
