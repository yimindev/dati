<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import type { McpPromptVO } from "~/api/mcp-prompt";
import { listPrompts } from "~/api/mcp-prompt";
import PromptList from "./PromptList.vue";

const { t } = useI18n();
const props = defineProps<{ serviceId: string }>();

const loading = ref(false);
const prompts = ref<McpPromptVO[]>([]);

const loadPrompts = async () => {
  try { loading.value = true; prompts.value = await listPrompts(props.serviceId); }
  finally { loading.value = false; }
};

defineExpose({ loadPrompts });
onMounted(loadPrompts);
</script>

<template>
  <div v-loading="loading" class="flex flex-col gap-4">
    <div>
      <h2 class="text-base font-semibold text-[var(--ep-text-color-primary)]">{{ t("mcpService.prompt.title") }}</h2>
      <p class="mt-1 text-[13px] text-[var(--ep-text-color-secondary)]">{{ t("mcpService.prompt.subtitle") }}</p>
    </div>
    <PromptList :prompts="prompts" :service-id="props.serviceId" @refresh="loadPrompts" />
  </div>
</template>
