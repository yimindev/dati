<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import type { McpServiceVO, McpServicePayload } from "~/api/mcp-service";
import { createMcpService, updateMcpService } from "~/api/mcp-service";

const { t } = useI18n();

interface Props {
  modelValue: boolean;
  service?: McpServiceVO | null;
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const formRef = ref();
const submitting = ref(false);

const formData = ref<McpServicePayload>({
  name: "",
  description: "",
});

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const isEdit = computed(() => !!props.service);

watch(
  () => props.service,
  (newVal) => {
    if (newVal) {
      formData.value = {
        name: newVal.name,
        description: newVal.description || "",
      };
    } else {
      resetForm();
    }
  },
  { immediate: true },
);

function resetForm() {
  formData.value = {
    name: "",
    description: "",
  };
  formRef.value?.resetValidation();
}

const handleSubmit = async () => {
  try {
    const valid = await formRef.value?.validate?.();
    if (!valid) return;

    submitting.value = true;

    if (isEdit.value) {
      await updateMcpService(props.service!.id, formData.value);
      ElMessage.success(t("common.saveSuccess"));
    } else {
      await createMcpService(formData.value);
      ElMessage.success(t("common.saveSuccess"));
    }

    emit("success");
  } catch (error) {
    console.error("提交失败:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    submitting.value = false;
  }
};

const handleCancel = () => {
  visible.value = false;
};
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? t('mcpService.editTitle') : t('mcpService.createTitle')"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <p class="dialog-note">
      {{ t("mcpService.dialogNote") }}
    </p>
    <McpServiceForm ref="formRef" v-model="formData" />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ t("common.cancel") }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? t("common.update") : t("common.create") }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-note {
  margin: -4px 0 18px;
  color: var(--ep-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}
</style>
