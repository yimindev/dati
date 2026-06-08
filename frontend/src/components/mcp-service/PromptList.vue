<script setup lang="ts">
import { ref, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Search, Edit, Delete } from "@element-plus/icons-vue";
import type { McpPromptVO } from "~/api/mcp-prompt";
import { updatePrompt, deletePrompt } from "~/api/mcp-prompt";
import PromptDialog from "./PromptDialog.vue";

const { t } = useI18n();
const props = defineProps<{ prompts: McpPromptVO[]; serviceId: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();

const searchQuery = ref("");
const dialogVisible = ref(false);
const editingPrompt = ref<McpPromptVO | null>(null);

const filteredPrompts = computed(() => {
  const q = searchQuery.value.toLowerCase();
  if (!q) return props.prompts;
  return props.prompts.filter(p => p.name.toLowerCase().includes(q) || (p.description || "").toLowerCase().includes(q));
});

const handleToggle = async (prompt: McpPromptVO) => {
  try {
    await updatePrompt(props.serviceId, prompt.id, {
      name: prompt.name, content: prompt.content,
      enabled: !prompt.enabled, parameters: prompt.parameters,
    });
    prompt.enabled = !prompt.enabled;
    ElMessage.success(t("common.saveSuccess"));
    emit("refresh");
  } catch (e: any) { ElMessage.error(e?.message || t("common.operationFailed")); }
};

const handleCreate = () => { editingPrompt.value = null; dialogVisible.value = true; };
const handleEdit = (prompt: McpPromptVO) => { editingPrompt.value = { ...prompt }; dialogVisible.value = true; };

const handleDelete = async (prompt: McpPromptVO) => {
  try {
    await ElMessageBox.confirm(t("mcpService.prompt.deleteConfirm", { name: prompt.name }), t("common.warning"),
      { confirmButtonText: t("common.confirm"), cancelButtonText: t("common.cancel"), type: "warning" });
    await deletePrompt(props.serviceId, prompt.id);
    ElMessage.success(t("common.deleteSuccess"));
    emit("refresh");
  } catch (e: any) { if (e !== "cancel") ElMessage.error(e?.message || t("common.operationFailed")); }
};

const handleDialogSaved = () => { dialogVisible.value = false; emit("refresh"); };
</script>

<template>
  <div class="flex flex-col gap-3">
    <div class="flex items-center gap-3 px-3.5 py-2.5 border border-[var(--ep-border-color-lighter)] rounded-lg bg-[var(--ep-bg-color)]">
      <el-input v-model="searchQuery" :placeholder="t('mcpService.prompt.searchPlaceholder')" clearable class="search-input" :prefix-icon="Search" />
      <span class="text-[13px] text-[var(--ep-text-color-secondary)] flex-1">{{ t("mcpService.prompt.totalCount", { count: props.prompts.length }) }}</span>
      <el-button type="primary" :icon="Plus" @click="handleCreate">{{ t("mcpService.prompt.addPrompt") }}</el-button>
    </div>

    <el-empty v-if="filteredPrompts.length === 0 && searchQuery" :description="t('mcpService.prompt.emptySearch')" />
    <el-empty v-else-if="filteredPrompts.length === 0" :description="t('mcpService.prompt.empty')" />

    <div v-else class="border border-[var(--ep-border-color-lighter)] rounded-lg bg-[var(--ep-bg-color)] overflow-hidden">
      <div v-for="prompt in filteredPrompts" :key="prompt.id" class="flex items-start gap-4 px-[18px] py-4 border-b border-[var(--ep-border-color-lighter)] last:border-b-0">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-sm font-semibold text-[var(--ep-text-color-primary)]">{{ prompt.name }}</span>
            <span class="text-xs text-[var(--ep-text-color-secondary)]">{{ t("mcpService.prompt.paramCount", { count: prompt.parameters?.length ?? 0 }) }}</span>
          </div>
          <p class="text-[13px] text-[var(--ep-text-color-secondary)]">{{ prompt.description }}</p>
        </div>
        <div class="flex items-center gap-3.5 shrink-0">
          <el-icon class="action-icon" @click="handleEdit(prompt)"><Edit /></el-icon>
          <el-icon class="action-icon action-delete" @click="handleDelete(prompt)"><Delete /></el-icon>
          <el-switch :model-value="prompt.enabled" @change="handleToggle(prompt)" />
        </div>
      </div>
    </div>

    <PromptDialog v-model="dialogVisible" :service-id="props.serviceId" :prompt="editingPrompt" @saved="handleDialogSaved" />
  </div>
</template>

<style scoped>
.action-icon { cursor: pointer; color: var(--ep-text-color-secondary); font-size: 17px; transition: color 0.15s; }
.action-icon:hover { color: var(--ep-color-primary); }
.action-delete:hover { color: var(--ep-color-danger); }
.search-input { width: 240px; }
</style>
