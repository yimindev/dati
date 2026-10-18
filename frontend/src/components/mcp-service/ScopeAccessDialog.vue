<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { FormInstance, FormRules } from "element-plus";
import type { DataScopeAccessMode, DataScopeItem } from "~/api/mcp-service";
import { getSchemas } from "~/api/datasource";
import { notifyError } from "~/api/http";
import { accessModeOf, normalizeAllowedSchemas } from "~/utils/data-scope";

const { t } = useI18n();

interface Props {
  modelValue: boolean;
  item: DataScopeItem | null;
  saving?: boolean;
}

interface Emits {
  (e: "update:modelValue", value: boolean): void;
  (
    e: "confirm",
    payload: { access_mode: DataScopeAccessMode; allowed_schemas: string[] },
  ): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const formRef = ref<FormInstance>();
const loadingSchemas = ref(false);
const schemaOptions = ref<string[]>([]);

const form = reactive<{
  access_mode: DataScopeAccessMode;
  allowed_schemas: string[];
}>({
  access_mode: "IMPORTED_TABLES",
  allowed_schemas: [],
});

const isSchemaScope = computed(() => form.access_mode === "SCHEMA_SCOPE");

const modeOptions = computed(() => [
  {
    value: "IMPORTED_TABLES" as const,
    label: t("mcpService.dataScope.modeImported"),
    description: t("mcpService.dataScope.modeImportedDesc"),
  },
  {
    value: "SCHEMA_SCOPE" as const,
    label: t("mcpService.dataScope.modeSchemaScope"),
    description: t("mcpService.dataScope.modeSchemaScopeDesc"),
  },
]);

const rules: FormRules = {
  allowed_schemas: [
    {
      validator: (
        _rule: unknown,
        value: string[],
        callback: (error?: Error) => void,
      ) => {
        if (isSchemaScope.value && (!value || value.length === 0)) {
          callback(new Error(t("mcpService.dataScope.allowedSchemasRequired")));
          return;
        }
        callback();
      },
      trigger: "change",
    },
  ],
};

const loadSchemas = async () => {
  if (!props.item) return;

  loadingSchemas.value = true;
  try {
    const schemas = await getSchemas(props.item.reference_id);
    schemaOptions.value = Array.from(
      new Set([...normalizeAllowedSchemas(schemas), ...form.allowed_schemas]),
    );
  } catch (error) {
    console.error("Failed to load schemas:", error);
    notifyError(error, t("mcpService.dataScope.schemaLoadFailed"));
    schemaOptions.value = [...form.allowed_schemas];
  } finally {
    loadingSchemas.value = false;
  }
};

watch(
  () => props.modelValue,
  (open) => {
    if (!open || !props.item) return;

    form.access_mode = accessModeOf(props.item);
    form.allowed_schemas = isSchemaScope.value
      ? normalizeAllowedSchemas(props.item.allowed_schemas)
      : [];
    schemaOptions.value = [...form.allowed_schemas];
    formRef.value?.clearValidate();
    loadSchemas();
  },
  { immediate: true },
);

watch(
  () => form.access_mode,
  () => formRef.value?.clearValidate("allowed_schemas"),
);

const handleCancel = () => {
  visible.value = false;
};

const handleConfirm = async () => {
  if (!props.item || !formRef.value) return;

  try {
    const valid = await formRef.value.validate();
    if (!valid) return;
  } catch {
    return;
  }

  emit("confirm", {
    access_mode: form.access_mode,
    allowed_schemas:
      form.access_mode === "SCHEMA_SCOPE"
        ? normalizeAllowedSchemas(form.allowed_schemas)
        : [],
  });
};
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('mcpService.dataScope.accessDialogTitle')"
    width="min(540px, 92vw)"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('mcpService.dataScope.accessMode')">
        <div class="flex flex-col gap-2.5 w-full">
          <div
            v-for="option in modeOptions"
            :key="option.value"
            class="mode-card"
            :class="{ active: form.access_mode === option.value }"
            @click="form.access_mode = option.value"
          >
            <div class="flex items-start gap-3">
              <el-radio
                :model-value="form.access_mode"
                :value="option.value"
                class="mode-radio"
              />
              <div class="flex flex-col gap-0.5 flex-1 min-w-0">
                <span class="mode-title">{{ option.label }}</span>
                <span class="mode-desc">{{ option.description }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-form-item>

      <el-form-item
        v-if="isSchemaScope"
        prop="allowed_schemas"
        :label="t('mcpService.dataScope.allowedSchemas')"
      >
        <el-select
          v-model="form.allowed_schemas"
          multiple
          filterable
          allow-create
          default-first-option
          clearable
          collapse-tags
          collapse-tags-tooltip
          :max-collapse-tags="3"
          class="w-full"
          :loading="loadingSchemas"
          :placeholder="t('mcpService.dataScope.allowedSchemasPlaceholder')"
        >
          <el-option
            v-for="schema in schemaOptions"
            :key="schema"
            :label="schema"
            :value="schema"
          />
          <template #empty>
            <div
              class="px-3 py-4 text-center text-xs text-[var(--ep-text-color-secondary)]"
            >
              {{
                loadingSchemas
                  ? t("common.loading")
                  : t("mcpService.dataScope.noSchemas")
              }}
            </div>
          </template>
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button :disabled="saving" @click="handleCancel">
          {{ t("common.cancel") }}
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleConfirm">
          {{ t("common.confirm") }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.mode-card {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  background-color: var(--ep-bg-color);
  transition: all 0.2s ease;
}

.mode-card:hover {
  border-color: var(--ep-color-primary-light-5, #a0cfff);
}

.mode-card.active {
  border-color: var(--ep-color-primary);
  background-color: var(--ep-color-primary-light-9, #ecf5ff);
}

.mode-radio {
  margin-right: 0;
  height: auto;
  margin-top: 2px;
}

.mode-radio :deep(.el-radio__label) {
  display: none;
}

.mode-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ep-text-color-primary);
}

.mode-desc {
  font-size: 12px;
  line-height: 1.5;
  color: var(--ep-text-color-secondary);
}
</style>
