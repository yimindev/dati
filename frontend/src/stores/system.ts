import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { getSystemConfig, type SystemConfig } from "~/api/system";

export const useSystemStore = defineStore("system", () => {
  // State
  const config = ref<SystemConfig | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const columnValueSampleLimit = computed(() => 
    config.value?.column_value_sample_limit ?? 1000
  );
  const columnValueLengthLimit = computed(() => 
    config.value?.column_value_length_limit ?? 256
  );
  const isLoaded = computed(() => config.value !== null);

  // Actions
  const loadConfig = async () => {
    if (isLoaded.value) return; // 已加载则跳过
    
    loading.value = true;
    error.value = null;
    
    try {
      config.value = await getSystemConfig();
    } catch (err) {
      error.value = err instanceof Error ? err.message : "Failed to load config";
      console.error("Failed to load system config:", err);
    } finally {
      loading.value = false;
    }
  };

  const refreshConfig = async () => {
    config.value = null; // 强制刷新
    await loadConfig();
  };

  return {
    // State
    config,
    loading,
    error,
    // Getters
    columnValueSampleLimit,
    columnValueLengthLimit,
    isLoaded,
    // Actions
    loadConfig,
    refreshConfig,
  };
});
