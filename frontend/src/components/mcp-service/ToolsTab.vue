<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import type { McpToolVO, ToolsResponse } from "~/api/mcp-tool";
import { listTools } from "~/api/mcp-tool";
import PrebuiltToolList from "./PrebuiltToolList.vue";
import CustomToolList from "./CustomToolList.vue";

const { t } = useI18n();
const props = defineProps<{ serviceId: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();

const loading = ref(false);
const prebuiltTools = ref<McpToolVO[]>([]);
const customTools = ref<McpToolVO[]>([]);
const activeSubTab = ref<"prebuilt" | "custom">("prebuilt");


const loadTools = async () => {
  try {
    loading.value = true;
    const data: ToolsResponse = await listTools(props.serviceId);
    prebuiltTools.value = data.prebuilt || [];
    customTools.value = data.custom || [];
  } finally {
    loading.value = false;
  }
};

const handleListRefresh = async () => {
  await loadTools();
  emit("refresh");
};

defineExpose({ loadTools });
onMounted(loadTools);
</script>

<template>
  <div v-loading="loading" class="flex flex-col gap-4">
    <!-- Sub Tabs -->
    <div class="sub-tabs flex items-center gap-6 w-full">
        <button
          class="sub-tab"
          :class="{ active: activeSubTab === 'prebuilt' }"
          @click="activeSubTab = 'prebuilt'"
        >
          {{ t("mcpService.tool.prebuiltTitle") }}
          <span class="count">{{ prebuiltTools.length }}</span>
        </button>
        <button
          class="sub-tab"
          :class="{ active: activeSubTab === 'custom' }"
          @click="activeSubTab = 'custom'"
        >
          {{ t("mcpService.tool.customTitle") }}
          <span class="count">{{ customTools.length }}</span>
        </button>
      </div>

    <PrebuiltToolList
      v-if="activeSubTab === 'prebuilt'"
      :tools="prebuiltTools"
      :service-id="props.serviceId"
      @refresh="handleListRefresh"
    />

    <CustomToolList
      v-else
      :tools="customTools"
      :service-id="props.serviceId"
      @refresh="handleListRefresh"
    />
  </div>
</template>

<style scoped>
.sub-tabs {
  display: flex;
  gap: 24px;
  width: 100%;
  border-bottom: 1px solid var(--ep-border-color-lighter);
}

.sub-tab {
  padding: 0 4px 10px;
  margin-bottom: -1px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ep-text-color-secondary);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sub-tab:hover {
  color: var(--ep-text-color-primary);
}

.sub-tab.active {
  color: var(--ep-color-primary);
  border-bottom-color: var(--ep-color-primary);
}

.count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  min-width: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  background: var(--ep-fill-color);
  color: var(--ep-text-color-secondary);
}

.sub-tab.active .count {
  background: var(--ep-color-primary-light-9);
  color: var(--ep-color-primary);
}
</style>
