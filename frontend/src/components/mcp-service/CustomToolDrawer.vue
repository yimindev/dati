<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { Delete, Plus } from "@element-plus/icons-vue";
import TemplatePreviewModal from "./TemplatePreviewModal.vue";
import type { McpToolVO, ToolParameter } from "~/api/mcp-tool";
import { createCustomTool, updateTool } from "~/api/mcp-tool";
import { getDataScope } from "~/api/mcp-service";

const { t } = useI18n();

const props = defineProps<{
  modelValue: boolean;
  serviceId: string;
  tool: McpToolVO | null;
}>();
const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
  (e: "saved"): void;
}>();

const saving = ref(false);
const formRef = ref<FormInstance>();
const dataSources = ref<{ id: string; name: string }[]>([]);
const previewVisible = ref(false);

const isEdit = computed(() => !!props.tool);

const paramTypes = ["String", "Number", "Boolean", "Date", "Array"];
const sqlOps = [
  { key: "allow_select", label: "SELECT" },
  { key: "allow_insert", label: "INSERT" },
  { key: "allow_update", label: "UPDATE" },
  { key: "allow_delete", label: "DELETE" },
  { key: "allow_ddl", label: "DDL" },
];

const rules: FormRules = {
  name: [
    { required: true, message: () => t("mcpService.tool.nameRequired"), trigger: "blur" },
  ],
  description: [
    { required: true, message: () => t("mcpService.tool.descRequired"), trigger: "blur" },
  ],
  sqlTemplate: [
    { required: true, message: () => t("mcpService.tool.sqlRequired"), trigger: "blur" },
  ],
  dataSourceId: [
    { required: true, message: () => t("mcpService.tool.dataSourceRequired"), trigger: "blur" },
  ],
};

const form = reactive({
  name: "",
  title: "",
  description: "",
  dataSourceId: "",
  sqlTemplate: "",
  parameters: [] as ToolParameter[],
  allowSelect: true,
  allowInsert: false,
  allowUpdate: false,
  allowDelete: false,
  allowDdl: false,
  maxRows: 1000,
  timeout: 30,
  confirmRequired: false,
});

const loadForm = () => {
  if (props.tool) {
    const cfg = props.tool.config as any;
    form.name = props.tool.name;
    form.title = props.tool.title || "";
    form.description = props.tool.description || "";
    form.dataSourceId = cfg?.data_source_id || "";
    form.sqlTemplate = cfg?.sql_template || "";
    form.parameters = cfg?.parameters ? [...cfg.parameters] : [];
    if (cfg?.sql_policy) {
      form.allowSelect = cfg.sql_policy.allow_select;
      form.allowInsert = cfg.sql_policy.allow_insert;
      form.allowUpdate = cfg.sql_policy.allow_update;
      form.allowDelete = cfg.sql_policy.allow_delete;
      form.allowDdl = cfg.sql_policy.allow_ddl;
    }
    form.maxRows = cfg?.max_rows ?? 1000;
    form.timeout = cfg?.timeout ?? 30;
    form.confirmRequired = cfg?.confirm_required ?? false;
  } else {
    Object.assign(form, {
      name: "",
      title: "",
      description: "",
      dataSourceId: "",
      sqlTemplate: "",
      parameters: [],
      allowSelect: true,
      allowInsert: false,
      allowUpdate: false,
      allowDelete: false,
      allowDdl: false,
      maxRows: 1000,
      timeout: 30,
      confirmRequired: false,
    });
  }
  formRef.value?.clearValidate();
};

const loadDataSources = async () => {
  try {
    const resp = await getDataScope(props.serviceId);
    dataSources.value = (resp.items || [])
      .filter((item: any) => item.scope_type === "DATA_SOURCE")
      .map((item: any) => ({
        id: item.reference_id,
        name: item.reference_name,
      }));
  } catch {
    // Ignore
  }
};

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      loadForm();
      loadDataSources();
    }
  },
);

const addParam = () => {
  form.parameters.push({ name: "", type: "String", required: false, description: "" });
};

const removeParam = (i: number) => {
  form.parameters.splice(i, 1);
};

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  saving.value = true;
  try {
    const config = JSON.stringify({
      data_source_id: form.dataSourceId,
      sql_template: form.sqlTemplate,
      parameters: form.parameters,
      sql_policy: {
        allow_select: form.allowSelect,
        allow_insert: form.allowInsert,
        allow_update: form.allowUpdate,
        allow_delete: form.allowDelete,
        allow_ddl: form.allowDdl,
        allow_multi: false,
      },
      timeout: form.timeout,
      max_rows: form.maxRows,
      confirm_required: form.confirmRequired,
    });

    if (isEdit.value) {
      await updateTool(props.serviceId, props.tool!.id, {
        tool_type: props.tool!.tool_type,
        name: form.name.trim(),
        title: form.title || undefined,
        description: form.description.trim() || undefined,
        enabled: props.tool!.enabled,
        config,
      });
    } else {
      await createCustomTool(props.serviceId, {
        tool_type: "PARAMETERIZED_SQL",
        name: form.name.trim(),
        title: form.title || undefined,
        description: form.description.trim() || undefined,
        config,
      });
    }
    ElMessage.success(t("common.saveSuccess"));
    emit("saved");
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="isEdit ? t('mcpService.tool.editCustom') : t('mcpService.tool.addCustom')"
    size="560px"
    :close-on-click-modal="false"
    @closed="loadForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="flex flex-col gap-6">
      <!-- Basic Info -->
      <section class="form-section">
        <h4>{{ t("mcpService.tool.basicInfo") }}</h4>
        <div class="grid grid-cols-2 gap-3">
          <el-form-item prop="name">
            <template #label>
              {{ t("mcpService.tool.toolName") }}
            </template>
            <el-input v-model="form.name" maxlength="128" placeholder="only_letters_and_123" />
            <span class="hint">{{ t("mcpService.tool.nameFormatHint") }}</span>
          </el-form-item>
          <el-form-item>
            <template #label>
              {{ t("mcpService.tool.toolTitle") }}
            </template>
            <el-input v-model="form.title" maxlength="255" :placeholder="t('mcpService.tool.titlePlaceholder')" />
          </el-form-item>
        </div>
        <el-form-item prop="description">
          <template #label>
            {{ t("common.description") }}
          </template>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            :placeholder="t('mcpService.tool.descPlaceholder')"
          />
        </el-form-item>
      </section>

      <!-- Execution Config -->
      <section class="form-section">
        <h4>{{ t("mcpService.tool.execConfig") }}</h4>
        <el-form-item prop="dataSourceId">
          <template #label>
            {{ t("mcpService.tool.dataSourceBinding") }}
          </template>
          <el-select
            v-model="form.dataSourceId"
            class="w-full"
            :placeholder="t('mcpService.tool.selectDataSource')"
          >
            <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="sqlTemplate">
          <template #label>
            {{ t("mcpService.tool.sqlTemplate") }}
          </template>
          <el-input
            v-model="form.sqlTemplate"
            type="textarea"
            :rows="4"
            placeholder="SELECT * FROM table WHERE id = :id"
            class="mono-input"
          />
          <div class="flex justify-end mt-1">
            <el-button size="small" @click="previewVisible = true">
              {{ t("mcpService.tool.previewRender") }}
            </el-button>
          </div>
        </el-form-item>
      </section>

      <!-- Parameters -->
      <section class="form-section">
        <div class="section-header">
          <h4>{{ t("mcpService.tool.parameters") }}</h4>
          <el-button size="small" text :icon="Plus" @click="addParam">
            {{ t("mcpService.tool.addParam") }}
          </el-button>
        </div>
        <div v-if="form.parameters.length === 0" class="empty-params">
          {{ t("mcpService.tool.noParams") }}
        </div>
        <div v-for="(param, i) in form.parameters" :key="i" class="param-row">
          <el-input v-model="param.name" size="small" placeholder="name" />
          <el-select v-model="param.type" size="small">
            <el-option v-for="pt in paramTypes" :key="pt" :label="pt" :value="pt" />
          </el-select>
          <el-input v-model="param.default_value" size="small" placeholder="default" />
          <el-checkbox v-model="param.required" size="small" class="param-required">
            {{ t("mcpService.tool.paramRequired") }}
          </el-checkbox>
          <el-button size="small" text type="danger" :icon="Delete" @click="removeParam(i)" />
          <el-input
            v-model="param.description"
            size="small"
            :placeholder="t('mcpService.tool.paramDesc')"
            class="param-desc"
          />
        </div>
      </section>

      <!-- Security -->
      <section class="form-section">
        <h4>{{ t("mcpService.tool.security") }}</h4>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="op in sqlOps"
            :key="op.key"
            type="button"
            class="perm-pill"
            :class="{ active: (form as any)[op.key] }"
            @click="(form as any)[op.key] = !(form as any)[op.key]"
          >
            {{ op.label }}
          </button>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div class="flex flex-col gap-1">
            <label>{{ t("mcpService.tool.maxRows") }}</label>
            <el-input-number v-model="form.maxRows" :min="1" :max="100000" size="small" />
          </div>
          <div class="flex flex-col gap-1">
            <label>{{ t("mcpService.tool.timeout") }} (s)</label>
            <el-input-number v-model="form.timeout" :min="1" :max="300" size="small" />
          </div>
        </div>
        <el-checkbox v-model="form.confirmRequired">
          {{ t("mcpService.tool.confirmRequired") }}
        </el-checkbox>
      </section>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        {{ t("common.save") }}
      </el-button>
    </template>
    <TemplatePreviewModal
      v-model="previewVisible"
      mode="SQL"
      :template="form.sqlTemplate"
      :parameters="form.parameters"
    />
  </el-drawer>
</template>

<style scoped>
.form-section { display: flex; flex-direction: column; gap: 12px; }
.form-section h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 650;
  color: var(--ep-text-color-primary);
  border-bottom: 1px solid var(--ep-border-color-lighter);
  padding-bottom: 6px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ep-border-color-lighter);
  padding-bottom: 6px;
}
.section-header h4 { margin: 0; border: none; padding: 0; }
.form-field label { font-size: 13px; font-weight: 500; color: var(--ep-text-color-primary); }
.hint { font-size: 11px; color: var(--ep-text-color-placeholder); }
.perm-pill {
  padding: 4px 12px;
  border: 1.5px solid var(--ep-border-color);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  background: var(--ep-bg-color);
  color: var(--ep-text-color-regular);
}
.perm-pill.active {
  background: var(--ep-color-primary-light-9);
  border-color: var(--ep-color-primary);
  color: var(--ep-color-primary);
  font-weight: 600;
}
.empty-params {
  text-align: center;
  padding: 20px;
  color: var(--ep-text-color-secondary);
  font-size: 13px;
  border: 1px dashed var(--ep-border-color);
  border-radius: 6px;
}
.param-row {
  display: grid;
  grid-template-columns: 1fr 100px 100px auto 32px;
  gap: 6px;
  align-items: start;
  padding: 10px;
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 6px;
  background: var(--ep-fill-color-lighter);
}
.param-desc { grid-column: 1 / -2; }
.param-required { padding-top: 4px; }
.mono-input :deep(textarea) { font-family: "Consolas", "Monaco", monospace; }
</style>
