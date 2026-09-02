<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import SqlSecurityConfig from "./SqlSecurityConfig.vue";
import type { McpToolVO, SqlPolicy } from "~/api/mcp-tool";
import { notifyError } from "~/api/http";
import { updateTool } from "~/api/mcp-tool";

const { t } = useI18n();

const props = defineProps<{ modelValue: boolean; serviceId: string; tool: McpToolVO }>();
const emit = defineEmits<{ (e: "update:modelValue", v: boolean): void; (e: "saved"): void }>();

const saving = ref(false);

const form = reactive({
  allowSelect: true,
  allowInsert: false,
  allowUpdate: false,
  allowDelete: false,
  allowDdl: false,
  allowMetadata: false,
  allowTransaction: false,
  allowSet: false,
  allowMulti: false,
  maxRows: 1000,
  timeout: 30,
});

const initForm = () => {
  const cfg = props.tool.config as any;
  if (cfg?.sql_policy) {
    const p = cfg.sql_policy as SqlPolicy;
    form.allowSelect = p.allow_select;
    form.allowInsert = p.allow_insert;
    form.allowUpdate = p.allow_update;
    form.allowDelete = p.allow_delete;
    form.allowDdl = p.allow_ddl;
    form.allowMetadata = p.allow_metadata ?? false;
    form.allowTransaction = p.allow_transaction ?? false;
    form.allowSet = p.allow_set ?? false;
    form.allowMulti = p.allow_multi;
  }
  form.maxRows = cfg?.max_rows ?? 1000;
  form.timeout = cfg?.timeout ?? 30;
};

watch(() => props.modelValue, (v) => { if (v) initForm(); }, { immediate: true });

const handleSave = async () => {
  saving.value = true;
  try {
    const existing = (props.tool.config as any) || {};
    const config = {
      ...existing,
      sql_policy: {
        ...(existing.sql_policy || {}),
        allow_select: form.allowSelect,
        allow_insert: form.allowInsert,
        allow_update: form.allowUpdate,
        allow_delete: form.allowDelete,
        allow_ddl: form.allowDdl,
        allow_metadata: form.allowMetadata,
        allow_transaction: form.allowTransaction,
        allow_set: form.allowSet,
        allow_multi: form.allowMulti,
      },
      timeout: form.timeout,
      max_rows: form.maxRows,
    };
    await updateTool(props.serviceId, "EXECUTE_SQL", {
      tool_type: "EXECUTE_SQL",
      enabled: props.tool.enabled,
      config: JSON.stringify(config),
    });
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
    :title="t('mcpService.tool.configExecuteSql')"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <SqlSecurityConfig
      v-model:allow-select="form.allowSelect"
      v-model:allow-insert="form.allowInsert"
      v-model:allow-update="form.allowUpdate"
      v-model:allow-delete="form.allowDelete"
      v-model:allow-ddl="form.allowDdl"
      v-model:allow-metadata="form.allowMetadata"
      v-model:allow-transaction="form.allowTransaction"
      v-model:allow-set="form.allowSet"
      v-model:allow-multi="form.allowMulti"
      v-model:max-rows="form.maxRows"
      v-model:timeout="form.timeout"
      :show-multi="true"
    />

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ t("common.save") }}</el-button>
    </template>
  </el-dialog>
</template>
