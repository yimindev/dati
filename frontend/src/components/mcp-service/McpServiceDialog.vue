<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Delete, Menu as IconMenu } from "@element-plus/icons-vue";
import type { McpServicePayload, DataScopeItem } from "~/api/mcp-service";
import { createMcpService } from "~/api/mcp-service";

const { t } = useI18n();

interface Props {
  modelValue: boolean;
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const formRef = ref();
const submitting = ref(false);
const scopePickerVisible = ref(false);

const formData = ref<McpServicePayload>({
  code: "",
  name: "",
  description: "",
  data_scopes: [],
});

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

// 打开时重置表单，避免二次打开残留上次数据（destroy-on-close 不重置父级状态）
watch(visible, (v) => {
  if (v) {
    formData.value = {
      code: "",
      name: "",
      description: "",
      data_scopes: [],
    };
    formRef.value?.resetValidation();
  }
});

const handleScopeConfirm = (newItems: DataScopeItem[]) => {
  // ScopePicker 已按 existingItems 置灰排除已选项，此处为防御性去重
  const existing = formData.value.data_scopes || [];
  const keys = new Set(existing.map((i) => `${i.scope_type}:${i.reference_id}`));
  const additions = newItems.filter((i) => !keys.has(`${i.scope_type}:${i.reference_id}`));
  formData.value.data_scopes = [...existing, ...additions];
};

const handleRemoveScope = (index: number) => {
  formData.value.data_scopes = (formData.value.data_scopes || []).filter((_, i) => i !== index);
};

const handleSubmit = async () => {
  try {
    const valid = await formRef.value?.validate?.();
    if (!valid) return;

    if ((formData.value.data_scopes?.length ?? 0) === 0) {
      ElMessage.warning(t("mcpService.create.dataScopeRequired"));
      return;
    }

    submitting.value = true;
    await createMcpService(formData.value);
    ElMessage.success(t("common.saveSuccess"));

    emit("success");
  } catch (error) {
    console.error("Failed to submit:", error);
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
    :title="t('mcpService.createTitle')"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <p class="dialog-note">
      {{ t("mcpService.dialogNote") }}
    </p>
    <McpServiceForm ref="formRef" v-model="formData" />

    <!-- Data scope section -->
    <div class="mt-5 border-t border-[var(--ep-border-color-lighter)] pt-4">
      <div class="flex items-center justify-between gap-4">
        <div>
          <h3 class="text-sm font-semibold text-[var(--ep-text-color-primary)]">
            <span class="required-mark">*</span>{{ t("mcpService.create.dataScopeSection") }}
          </h3>
          <span class="mt-0.5 block text-xs text-[var(--ep-text-color-secondary)]">
            {{ t("mcpService.dataScope.subtitle") }}
          </span>
        </div>
        <el-button :icon="Plus" @click="scopePickerVisible = true">
          {{ t("mcpService.create.addDataScope") }}
        </el-button>
      </div>

      <div
        v-if="(formData.data_scopes?.length ?? 0) === 0"
        class="mt-3 rounded-lg border border-dashed border-[var(--ep-border-color)] px-4 py-5 text-center text-xs text-[var(--ep-text-color-secondary)]"
      >
        {{ t("mcpService.create.emptyDataScope") }}
      </div>

      <div v-else class="mt-3 flex flex-col gap-2">
        <div
          v-for="(item, index) in formData.data_scopes"
          :key="`${item.scope_type}-${item.reference_id}`"
          class="flex items-center justify-between gap-3 rounded-lg border border-[var(--ep-border-color-lighter)] px-3 py-2"
        >
          <div class="flex min-w-0 items-center gap-2">
            <el-icon class="text-[var(--ep-color-primary)]">
              <span v-if="item.scope_type === 'DATA_SOURCE'" class="icon-[codicon--database]"></span>
              <IconMenu v-else />
            </el-icon>
            <span class="truncate text-sm text-[var(--ep-text-color-primary)]">
              {{ item.reference_name || item.reference_id }}
            </span>
            <el-tag size="small" :type="item.scope_type === 'DATA_SOURCE' ? 'primary' : 'success'">
              {{ item.scope_type === "DATA_SOURCE" ? t("common.dataSource") : t("common.subject") }}
            </el-tag>
          </div>
          <el-button link type="danger" :icon="Delete" @click="handleRemoveScope(index)">
            {{ t("common.delete") }}
          </el-button>
        </div>
      </div>
    </div>

    <ScopePicker
      v-model="scopePickerVisible"
      :existing-items="formData.data_scopes || []"
      @confirm="handleScopeConfirm"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ t("common.cancel") }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t("common.create") }}
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

.required-mark {
  margin-right: 4px;
  color: var(--ep-color-danger);
}
</style>
