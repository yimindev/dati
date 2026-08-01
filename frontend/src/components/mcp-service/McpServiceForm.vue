<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import type { FormInstance, FormRules } from "element-plus";
import type { McpServicePayload } from "~/api/mcp-service";

const { t } = useI18n();

interface Props {
  modelValue: McpServicePayload;
}

interface Emits {
  (e: "update:modelValue", value: McpServicePayload): void;
}

defineProps<Props>();
defineEmits<Emits>();

const formRef = ref<FormInstance>();

const rules: FormRules = {
  code: [
    {
      required: true,
      message: t("mcpService.codeRequired"),
      trigger: ["blur", "change"],
    },
    {
      pattern: /^[a-z0-9][a-z0-9_-]*$/,
      message: t("mcpService.codeFormatError"),
      trigger: ["blur", "change"],
    },
    {
      min: 1,
      max: 64,
      message: t("mcpService.codeFormatError"),
      trigger: ["blur", "change"],
    },
  ],
  name: [
    {
      required: true,
      message: t("common.required", { name: t("common.name") }),
      trigger: ["blur", "change"],
    },
    {
      min: 1,
      max: 100,
      message: t("common.nameLengthError"),
      trigger: ["blur", "change"],
    },
  ],
};

const validate = async () => {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
};

const resetValidation = () => {
  formRef.value?.clearValidate();
};

defineExpose({
  validate,
  resetValidation,
});
</script>

<template>
  <el-form
    ref="formRef"
    :model="modelValue"
    :rules="rules"
    label-position="top"
    class="mcp-service-form"
    @submit.prevent
  >
    <el-form-item :label="t('mcpService.serviceCode')" prop="code">
      <el-input
        v-model="modelValue.code"
        :placeholder="'my-service'"
        maxlength="64"
        show-word-limit
      />
      <div class="form-hint">{{ t("mcpService.codeHint") }}</div>
    </el-form-item>
    <el-form-item :label="t('common.name')" prop="name">
      <el-input
        v-model="modelValue.name"
        :placeholder="t('common.placeholder.name')"
        maxlength="100"
        show-word-limit
      />
    </el-form-item>
    <el-form-item :label="t('common.description')" prop="description">
      <el-input
        v-model="modelValue.description"
        :placeholder="t('common.placeholder.description')"
        :rows="4"
        type="textarea"
        maxlength="500"
        show-word-limit
      />
    </el-form-item>
  </el-form>
</template>

<style scoped>
.mcp-service-form :deep(.el-form-item__label) {
  align-items: center;
  color: var(--ep-text-color-primary);
  font-weight: 600;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ep-text-color-secondary);
  line-height: 1.4;
}
</style>
