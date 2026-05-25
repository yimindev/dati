<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { Delete, Plus } from "@element-plus/icons-vue";
import type { McpPromptVO, PromptParameter } from "~/api/mcp-prompt";
import { createPrompt, updatePrompt } from "~/api/mcp-prompt";

const { t } = useI18n();
const props = defineProps<{ modelValue: boolean; serviceId: string; prompt: McpPromptVO | null }>();
const emit = defineEmits<{ (e: "update:modelValue", v: boolean): void; (e: "saved"): void }>();

const saving = ref(false);
const formRef = ref<FormInstance>();
const isEdit = computed(() => !!props.prompt);

const rules: FormRules = {
  name: [{ required: true, message: () => t("mcpService.prompt.nameRequired"), trigger: "blur" }],
  content: [{ required: true, message: () => t("mcpService.prompt.contentRequired"), trigger: "blur" }],
};

const form = reactive({ name: "", description: "", content: "", parameters: [] as PromptParameter[] });

const loadForm = () => {
  if (props.prompt) {
    form.name = props.prompt.name; form.description = props.prompt.description || "";
    form.content = props.prompt.content || ""; form.parameters = props.prompt.parameters ? [...props.prompt.parameters] : [];
  } else {
    form.name = ""; form.description = ""; form.content = ""; form.parameters = [];
  }
  formRef.value?.clearValidate();
};
watch(() => props.modelValue, (v) => { if (v) loadForm(); });

const addParam = () => form.parameters.push({ name: "", description: "", required: false });
const removeParam = (i: number) => form.parameters.splice(i, 1);

const handleSave = async () => {
  if (!await formRef.value?.validate().catch(() => false)) return;
  saving.value = true;
  try {
    if (isEdit.value) {
      await updatePrompt(props.serviceId, props.prompt!.id, {
        name: form.name.trim(), description: form.description || undefined,
        enabled: props.prompt!.enabled, content: form.content, parameters: form.parameters,
      });
    } else {
      await createPrompt(props.serviceId, {
        name: form.name.trim(), description: form.description || undefined,
        content: form.content, parameters: form.parameters,
      });
    }
    ElMessage.success(t("common.saveSuccess")); emit("saved");
  } catch (e: any) { ElMessage.error(e?.message || t("common.operationFailed")); }
  finally { saving.value = false; }
};
</script>

<template>
  <el-drawer :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)"
    :title="isEdit ? t('mcpService.prompt.editPrompt') : t('mcpService.prompt.addPrompt')" size="560px" :close-on-click-modal="false" @closed="loadForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="flex flex-col gap-6">
      <section class="flex flex-col gap-3">
        <h4 class="text-sm font-semibold pb-2 border-b border-[var(--ep-border-color-lighter)]">{{ t("mcpService.prompt.basicInfo") }}</h4>
        <el-form-item prop="name">
          <template #label>{{ t("mcpService.prompt.promptName") }}</template>
          <el-input v-model="form.name" maxlength="128" placeholder="e.g. analyze_table" />
          <span class="text-[11px] text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.tool.nameFormatHint") }}</span>
        </el-form-item>
        <el-form-item>
          <template #label>{{ t("common.description") }}</template>
          <el-input v-model="form.description" :placeholder="t('mcpService.prompt.descPlaceholder')" />
        </el-form-item>
      </section>

      <section class="flex flex-col gap-3">
        <div class="section-header">
          <h4 class="text-sm font-semibold">{{ t("mcpService.prompt.parameters") }}</h4>
          <el-button size="small" text :icon="Plus" native-type="button" @click="addParam">{{ t("mcpService.tool.addParam") }}</el-button>
        </div>
        <div v-if="form.parameters.length === 0" class="empty-params">{{ t("mcpService.prompt.noParams") }}</div>
        <div v-for="(p, i) in form.parameters" :key="i" class="param-row">
          <el-input v-model="p.name" size="small" placeholder="param_name" />
          <el-input v-model="p.description" size="small" :placeholder="t('mcpService.tool.paramDesc')" class="param-desc" />
          <el-checkbox v-model="p.required" size="small" class="param-required">{{ t("mcpService.tool.paramRequired") }}</el-checkbox>
          <el-button size="small" text type="danger" :icon="Delete" @click="removeParam(i)" />
        </div>
      </section>

      <section class="flex flex-col gap-3">
        <h4 class="text-sm font-semibold pb-2 border-b border-[var(--ep-border-color-lighter)]">{{ t("mcpService.prompt.content") }}</h4>
        <el-form-item prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" :placeholder="t('mcpService.prompt.contentPlaceholder')" class="mono-input" />
          <span class="text-[11px] text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.tool.nameFormatHint") }}</span>
        </el-form-item>
      </section>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
      <el-button type="primary" :loading="saving" :disabled="!form.name.trim()" @click="handleSave">{{ t("common.save") }}</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.section-header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--ep-border-color-lighter); padding-bottom: 6px; }
.empty-params { text-align: center; padding: 20px; color: var(--ep-text-color-secondary); font-size: 13px; border: 1px dashed var(--ep-border-color); border-radius: 6px; }
.param-row { display: grid; grid-template-columns: 1fr 2fr auto 32px; gap: 6px; align-items: start; padding: 10px; border: 1px solid var(--ep-border-color-lighter); border-radius: 6px; background: var(--ep-fill-color-lighter); }
.param-required { padding-top: 4px; }
.mono-input :deep(textarea) { font-family: "Consolas", "Monaco", monospace; }
</style>
