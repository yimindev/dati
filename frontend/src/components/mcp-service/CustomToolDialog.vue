<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { Delete, Plus, InfoFilled, MagicStick } from "@element-plus/icons-vue";
import type { McpToolVO, ToolParameter } from "~/api/mcp-tool";
import { createCustomTool, updateTool, detectToolAnnotations } from "~/api/mcp-tool";
import { notifyError } from "~/api/http";
import { getDataScope } from "~/api/mcp-service";
import { extractTemplateVariables } from "~/api/template-preview";

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
const scrollBodyRef = ref<HTMLElement>();
const dataSources = ref<{ id: string; name: string }[]>([]);
const previewVisible = ref(false);

const isEdit = computed(() => !!props.tool);

const paramTypes = ["String", "Number", "Boolean", "DateTime", "Array"];
const rules: FormRules = {
  name: [
    { required: true, message: () => t("mcpService.tool.nameRequired"), trigger: "blur" },
  ],
  description: [
    { required: true, message: () => t("mcpService.tool.descRequired"), trigger: "blur" },
  ],
  dataSourceId: [
    { required: true, message: () => t("mcpService.tool.dataSourceRequired"), trigger: "blur" },
  ],
  sqlTemplate: [
    { required: true, message: () => t("mcpService.tool.sqlRequired"), trigger: "blur" },
  ],
};

type TriState = "unset" | "true" | "false";

const toTriState = (v: boolean | null | undefined): TriState => {
  if (v === true) return "true";
  if (v === false) return "false";
  return "unset";
};

const fromTriState = (v: TriState): boolean | null => {
  if (v === "true") return true;
  if (v === "false") return false;
  return null;
};

const form = reactive({
  name: "",
  title: "",
  description: "",
  dataSourceId: "",
  sqlTemplate: "",
  parameters: [] as ToolParameter[],
  maxRows: 1000,
  timeout: 30,
  readOnly: "unset" as TriState,
  idempotent: "unset" as TriState,
  destructive: "unset" as TriState,
});

const triStateOptions = computed(() => [
  { label: t("mcpService.tool.annotationUnset"), value: "unset" },
  { label: t("mcpService.tool.annotationYes"), value: "true" },
  { label: t("mcpService.tool.annotationNo"), value: "false" },
]);

const loadForm = () => {
  if (props.tool) {
    const cfg = props.tool.config as any;
    form.name = props.tool.name;
    form.title = props.tool.title || "";
    form.description = props.tool.description || "";
    form.dataSourceId = cfg?.data_source_id || "";
    form.sqlTemplate = cfg?.sql_template || "";
    form.parameters = cfg?.parameters ? [...cfg.parameters] : [];
    form.maxRows = cfg?.max_rows ?? 1000;
    form.timeout = cfg?.timeout ?? 30;
    form.readOnly = toTriState(cfg?.read_only);
    form.idempotent = toTriState(cfg?.idempotent);
    form.destructive = toTriState(cfg?.destructive);
  } else {
    Object.assign(form, {
      name: "",
      title: "",
      description: "",
      dataSourceId: "",
      sqlTemplate: "",
      parameters: [],
      maxRows: 1000,
      timeout: 30,
      readOnly: "unset",
      idempotent: "unset",
      destructive: "unset",
    });
  }
  formRef.value?.clearValidate();
};

const loadDataSources = async () => {
  try {
    const resp = await getDataScope(props.serviceId);
    dataSources.value = (resp.resolved_data_sources || []).map((ds) => ({
      id: ds.id,
      name: ds.name,
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
      nextTick(() => {
        if (scrollBodyRef.value) {
          scrollBodyRef.value.scrollTop = 0;
        }
      });
    }
  },
);

const scanning = ref(false);
const scanParams = async () => {
  if (!form.sqlTemplate) {
    ElMessage.warning(t("mcpService.tool.sqlRequired"));
    return;
  }
  scanning.value = true;
  try {
    const { variables } = await extractTemplateVariables({ template: form.sqlTemplate });
    let addedCount = 0;
    variables.forEach((name) => {
      if (!form.parameters.find((p) => p.name === name)) {
        form.parameters.push({ name, type: "String", required: false, description: "" });
        addedCount++;
      }
    });

    if (addedCount > 0) {
      ElMessage.success(t("mcpService.tool.scanParamsSuccess", { count: addedCount }));
    } else {
      ElMessage.info(t("mcpService.tool.scanParamsNoNew"));
    }
  } catch (err: any) {
    notifyError(err, t("common.operationFailed"));
  } finally {
    scanning.value = false;
  }
};

const addParam = () => {
  form.parameters.push({ name: "", type: "String", required: false, description: "" });
};

const removeParam = (i: number) => {
  form.parameters.splice(i, 1);
};

const detecting = ref(false);
const handleDetectAnnotations = async () => {
  if (!form.sqlTemplate?.trim()) {
    ElMessage.warning(t("mcpService.tool.detectEmptySql"));
    return;
  }
  detecting.value = true;
  try {
    const resp = await detectToolAnnotations(props.serviceId, {
      template: form.sqlTemplate,
      parameters: form.parameters.map((p) => ({ name: p.name, type: p.type })),
    });
    form.readOnly = toTriState(resp.read_only);
    form.idempotent = toTriState(resp.idempotent);
    form.destructive = toTriState(resp.destructive);
    ElMessage.success(
      t("mcpService.tool.detectSuccess", { op: resp.detected_operation || "SQL" }),
    );
  } catch (err: any) {
    notifyError(err, t("common.operationFailed"));
  } finally {
    detecting.value = false;
  }
};

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (!form.sqlTemplate.trim()) {
    ElMessage.warning(t("mcpService.tool.sqlRequired"));
    return;
  }

  saving.value = true;
  try {
    const config = JSON.stringify({
      data_source_id: form.dataSourceId,
      sql_template: form.sqlTemplate,
      parameters: form.parameters,
      timeout: form.timeout,
      max_rows: form.maxRows,
      read_only: fromTriState(form.readOnly),
      idempotent: fromTriState(form.idempotent),
      destructive: fromTriState(form.destructive),
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
    notifyError(e, t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="isEdit ? t('mcpService.tool.editCustom') : t('mcpService.tool.addCustom')"
    width="780px"
    :close-on-click-modal="false"
    destroy-on-close
    append-to-body
  >
    <div ref="scrollBodyRef" class="dialog-body-scroll flex flex-col overflow-y-auto px-3 py-1">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- Basic Info -->
      <section class="flex flex-col mb-6">
        <h4 class="m-0 mb-4 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.tool.basicInfo") }}</h4>
        <div class="grid grid-cols-2 gap-3">
          <el-form-item prop="name">
            <template #label>
              {{ t("mcpService.tool.toolName") }}
            </template>
            <el-input v-model="form.name" maxlength="128" placeholder="only_letters_and_123" />
            <span class="block mt-1 text-[11px] leading-none text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.tool.nameFormatHint") }}</span>
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
            :autosize="{ minRows: 2, maxRows: 8 }"
            :placeholder="t('mcpService.tool.descPlaceholder')"
          />
        </el-form-item>
      </section>

      <!-- Execution Config -->
      <section class="flex flex-col mb-6">
        <h4 class="m-0 mb-4 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.tool.execConfig") }}</h4>
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
          <SqlTemplateEditor v-model="form.sqlTemplate" :label="t('mcpService.tool.sqlTemplate')" required />
        </el-form-item>
      </section>

      <!-- Parameters -->
      <section class="flex flex-col mb-6">
        <h4 class="m-0 mb-4 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.tool.parameters") }}</h4>
        <div class="flex gap-2 mb-4">
          <el-button size="small" :loading="scanning" @click="scanParams">
            {{ t("mcpService.tool.scanParams") }}
          </el-button>
          <el-button size="small" :icon="Plus" @click="addParam">
            {{ t("mcpService.tool.addParam") }}
          </el-button>
        </div>

        <el-table :data="form.parameters" size="small" border max-height="220" class="param-table">
          <el-table-column prop="name" :label="t('common.name')" width="180">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" placeholder="name" />
            </template>
          </el-table-column>
          <el-table-column prop="type" :label="t('common.type')" width="120">
            <template #default="{ row }">
              <el-select v-model="row.type" size="small">
                <el-option v-for="pt in paramTypes" :key="pt" :label="pt" :value="pt" />
              </el-select>
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
            <div class="py-4 text-[var(--ep-text-color-placeholder)]">{{ t("mcpService.tool.noParams") }}</div>
          </template>
        </el-table>
      </section>

      <!-- Execution Limits -->
      <section class="flex flex-col mb-6">
        <h4 class="m-0 mb-4 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">{{ t("mcpService.tool.execLimit") }}</h4>
        <div class="flex items-start gap-8">
          <div class="w-44">
            <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.maxRows") }}</label>
            <el-input-number v-model="form.maxRows" :min="1" :max="100000" size="small" class="w-full" />
          </div>
          <div class="w-44">
            <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.timeout") }} (s)</label>
            <el-input-number v-model="form.timeout" :min="1" :max="300" size="small" class="w-full" />
          </div>
        </div>
      </section>

      <!-- MCP Annotations -->
      <section class="flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <h4 class="m-0 text-sm font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-2">
            {{ t("mcpService.tool.annotationsSection") }}
          </h4>
          <el-button
            type="primary"
            link
            size="small"
            :icon="MagicStick"
            :loading="detecting"
            @click="handleDetectAnnotations"
          >
            {{ t("mcpService.tool.detectAnnotations") }}
          </el-button>
        </div>
        <div class="flex flex-wrap items-start gap-8">
          <!-- readOnly -->
          <div>
            <div class="flex items-center gap-1 mb-1.5">
              <label class="text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.readOnlyLabel") }}</label>
              <el-tooltip :content="t('mcpService.tool.readOnlyDesc')" placement="top">
                <el-icon class="text-[var(--ep-text-color-secondary)] cursor-pointer text-xs"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
            <el-radio-group v-model="form.readOnly">
              <el-radio v-for="opt in triStateOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
          </div>

          <!-- idempotent -->
          <div>
            <div class="flex items-center gap-1 mb-1.5">
              <label class="text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.idempotentLabel") }}</label>
              <el-tooltip :content="t('mcpService.tool.idempotentDesc')" placement="top">
                <el-icon class="text-[var(--ep-text-color-secondary)] cursor-pointer text-xs"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
            <el-radio-group v-model="form.idempotent">
              <el-radio v-for="opt in triStateOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
          </div>

          <!-- destructive -->
          <div>
            <div class="flex items-center gap-1 mb-1.5">
              <label class="text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.destructiveLabel") }}</label>
              <el-tooltip :content="t('mcpService.tool.destructiveDesc')" placement="top">
                <el-icon class="text-[var(--ep-text-color-secondary)] cursor-pointer text-xs"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
            <el-radio-group v-model="form.destructive">
              <el-radio v-for="opt in triStateOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
            </el-radio-group>
          </div>
        </div>
      </section>
    </el-form>
    </div>

    <TemplatePreviewDialog
      v-model="previewVisible"
      :template="form.sqlTemplate"
      mode="SQL"
      :parameters="form.parameters"
    />


    <template #footer>
      <div class="flex justify-end items-center gap-3">
        <el-button plain :disabled="form.parameters.length === 0" @click="previewVisible = true">
          {{ t("mcpService.tool.previewRender") }}
        </el-button>
        <el-divider direction="vertical" />
        <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ t("common.save") }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-body-scroll {
  max-height: calc(85vh - 180px);
}
/* ── h4 左侧蓝色装饰条（保留伪元素） ── */
h4::before {
  content: "";
  width: 3px;
  height: 14px;
  background-color: var(--ep-color-primary);
  border-radius: 2px;
}
/* ── el-table 单元格紧凑内边距 ── */
.param-table :deep(.el-table__cell) {
  padding: 8px 0;
}
</style>
