<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import {
  DocumentCopy,
  InfoFilled,
  Link,
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
  <aside class="mcp-access-card panel p-5 lg:col-span-5 flex flex-col gap-4 shadow-sm border border-[var(--ep-border-color-lighter)] rounded-xl bg-[var(--ep-bg-color)]">
    <!-- Header: 标题 -->
    <div class="flex items-center gap-2 pb-3 border-b border-[var(--ep-border-color-lighter)]">
      <el-icon class="text-[var(--ep-color-primary)]"><Link /></el-icon>
      <span class="text-sm font-semibold text-[var(--ep-text-color-primary)]">
        {{ t("mcpService.accessConfig.title") }}
      </span>
    </div>

    <!-- 1. 服务访问端点 (Endpoint Box) -->
    <div class="flex flex-col gap-1.5">
      <div class="flex items-center justify-between gap-2 flex-wrap">
        <label class="text-xs font-medium text-[var(--ep-text-color-secondary)]">
          {{ t("mcpService.accessConfig.endpointTitle") }}
        </label>
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-[11px] font-mono px-2 py-0.5 rounded bg-[var(--ep-color-primary-light-9)] text-[var(--ep-color-primary)] font-medium">
            Streamable HTTP
          </span>
          <span class="text-[11px] font-mono px-2 py-0.5 rounded bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-secondary)] border border-[var(--ep-border-color-lighter)]">
            MCP 2025-11-25
          </span>
          <span
            v-if="service?.status === 'DRAFT'"
            class="text-[11px] text-amber-500 flex items-center gap-1 font-medium"
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

    <!-- 2. 参考 JSON 配置 -->
    <div class="flex flex-col gap-2">
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold text-[var(--ep-text-color-primary)]">
          {{ t("mcpService.accessConfig.configTitle") }}
        </span>
        <el-tooltip :content="t('common.copy')" placement="top">
          <el-button
            link
            :icon="DocumentCopy"
            class="!p-0 !h-auto text-[var(--ep-text-color-secondary)] hover:text-[var(--ep-color-primary)]"
            @click="handleCopy(jsonConfigSnippet, t('mcpService.accessConfig.copySuccess'))"
          />
        </el-tooltip>
      </div>

      <pre class="m-0 font-mono text-xs text-[var(--ep-text-color-primary)] whitespace-pre-wrap break-all max-h-56 overflow-y-auto leading-relaxed select-all rounded-lg bg-[var(--ep-fill-color-light)] border border-[var(--ep-border-color-lighter)] p-3">{{ jsonConfigSnippet }}</pre>

      <p class="text-[11px] text-[var(--ep-text-color-secondary)] m-0 leading-normal">
        {{ t("mcpService.accessConfig.configGuide") }}
      </p>
    </div>

    <!-- 3. API Key 获取入口 -->
    <el-button
      type="primary"
      link
      :icon="Right"
      class="!p-0 !h-auto text-xs self-start"
      @click="goToApiKeys"
    >
      {{ t("mcpService.accessConfig.manageApiKeys") }}
    </el-button>
  </aside>
</template>

<style scoped>
.mcp-access-card {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}
</style>
