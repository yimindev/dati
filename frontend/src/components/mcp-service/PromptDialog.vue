<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { Delete, Plus } from "@element-plus/icons-vue";
import type { McpPromptVO, PromptParameter } from "~/api/mcp-prompt";
import { createPrompt, updatePrompt } from "~/api/mcp-prompt";
import { extractTemplateVariables } from "~/api/template-preview";

const { t } = useI18n();
const props = defineProps<{ modelValue: boolean; serviceId: string; prompt: McpPromptVO | null }>();
const emit = defineEmits<{ (e: "update:modelValue", v: boolean): void; (e: "saved"): void }>();

const saving = ref(false);
const previewVisible = ref(false);
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

const scanning = ref(false);
const scanParams = async () => {
  if (!form.content) {
    ElMessage.warning(t("mcpService.prompt.contentRequired"));
    return;
  }
  scanning.value = true;
  try {
    const { variables } = await extractTemplateVariables({ template: form.content });
    let addedCount = 0;
    variables.forEach((name) => {
      if (!form.parameters.find((p) => p.name === name)) {
        form.parameters.push({ name, description: "", required: false });
        addedCount++;
      }
    });
    if (addedCount > 0) ElMessage.success(t("mcpService.tool.scanParamsSuccess", { count: addedCount }));
    else ElMessage.info(t("mcpService.tool.scanParamsNoNew"));
  } catch (err: any) {
    ElMessage.error(err?.message || t("common.operationFailed"));
  } finally {
    scanning.value = false;
  }
};
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
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="isEdit ? t('mcpService.prompt.editPrompt') : t('mcpService.prompt.addPrompt')"
    width="780px"
    :close-on-click-modal="false"
    @close="loadForm"
    append-to-body
  >
    <div class="flex flex-col gap-8 overflow-y-auto px-3 py-1" style="max-height: calc(85vh - 180px)">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- Basic Info -->
      <section class="flex flex-col gap-4">
        <h4 class="m-0 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.prompt.basicInfo") }}</h4>
        <el-form-item prop="name">
          <template #label>{{ t("mcpService.prompt.promptName") }}</template>
          <el-input v-model="form.name" maxlength="128" placeholder="e.g. analyze_table" />
          <span class="block mt-1 text-[11px] text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.tool.nameFormatHint") }}</span>
        </el-form-item>
        <el-form-item>
          <template #label>{{ t("common.description") }}</template>
          <el-input v-model="form.description" :placeholder="t('mcpService.prompt.descPlaceholder')" />
        </el-form-item>
      </section>

      <!-- Content -->
      <section class="flex flex-col gap-4">
        <h4 class="m-0 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.prompt.content") }}</h4>
        <el-form-item prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" :placeholder="t('mcpService.prompt.contentPlaceholder')" class="mono-input" />
        </el-form-item>
      </section>

      <!-- Parameters -->
      <section class="flex flex-col gap-4">
        <h4 class="m-0 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.prompt.parameters") }}</h4>
        <div class="flex gap-2">
          <el-button size="small" :loading="scanning" @click="scanParams">{{ t("mcpService.tool.scanParams") }}</el-button>
          <el-button size="small" :icon="Plus" @click="addParam">{{ t("mcpService.tool.addParam") }}</el-button>
        </div>

        <el-table :data="form.parameters" size="small" border max-height="220" class="param-table">
          <el-table-column prop="name" :label="t('common.name')" width="180">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" placeholder="name" />
            </template>
          </el-table-column>
          <el-table-column prop="required" :label="t('mcpService.tool.paramRequired')" width="80" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.required" />
            </template>
          </el-table-column>
          <el-table-column prop="description" :label="t('common.description')">
            <template #default="{ row }">
              <el-input v-model="row.description" size="small" :placeholder="t('mcpService.tool.paramDesc')" />
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="80" align="center">
            <template #default="{ $index }">
              <el-button size="small" text type="danger" :icon="Delete" @click="removeParam($index)" />
            </template>
          </el-table-column>
          <template #empty>
            <div class="py-4 text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.prompt.noParams") }}</div>
          </template>
        </el-table>
      </section>
    </el-form>
    </div>

    <TemplatePreviewDialog
      v-model="previewVisible"
      :template="form.content"
      mode="TEXT"
      :parameters="form.parameters"
    />


    <template #footer>
      <div class="flex justify-end items-center gap-3">
        <el-button plain :disabled="form.parameters.length === 0" @click="previewVisible = true">
          {{ t("mcpService.prompt.previewRender") }}
        </el-button>
        <el-divider direction="vertical" />
        <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
        <el-button type="primary" :loading="saving" :disabled="!form.name.trim()" @click="handleSave">
          {{ t("common.save") }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
/* ── h4 左侧蓝色装饰条（保留伪元素） ── */
h4::before {
  content: "";
  width: 3px;
  height: 14px;
  background-color: var(--ep-color-primary);
  border-radius: 2px;
}
/* ── 等宽字体输入框（需穿透 el-input） ── */
.mono-input :deep(textarea) {
  font-family: "Fira Code", "Consolas", "Monaco", monospace;
}
/* ── el-table 单元格紧凑内边距 ── */
.param-table :deep(.el-table__cell) {
  padding: 8px 0;
}
</style>
