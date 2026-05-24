<script setup lang="ts">
import { ref, reactive, watch } from "vue";
import { ElMessage } from "element-plus";
import { WarningFilled } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import type { McpToolVO, SqlPolicy } from "~/api/mcp-tool";
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
  allowMulti: false,
  maxRows: 1000,
  timeout: 30,
  confirmRequired: false,
});

const ops = [
  { key: "allowSelect", label: "SELECT" },
  { key: "allowInsert", label: "INSERT" },
  { key: "allowUpdate", label: "UPDATE" },
  { key: "allowDelete", label: "DELETE" },
  { key: "allowDdl", label: "DDL" },
  { key: "allowMulti", label: "MULTI" },
];

const initForm = () => {
  const cfg = props.tool.config as any;
  if (cfg?.sql_policy) {
    const p = cfg.sql_policy as SqlPolicy;
    form.allowSelect = p.allow_select;
    form.allowInsert = p.allow_insert;
    form.allowUpdate = p.allow_update;
    form.allowDelete = p.allow_delete;
    form.allowDdl = p.allow_ddl;
    form.allowMulti = p.allow_multi;
  }
  form.maxRows = cfg?.max_rows ?? 1000;
  form.timeout = cfg?.timeout ?? 30;
  form.confirmRequired = cfg?.confirm_required ?? false;
};

watch(() => props.modelValue, (v) => { if (v) initForm(); }, { immediate: true });

const handleSave = async () => {
  saving.value = true;
  try {
    const config = {
      sql_policy: {
        allow_select: form.allowSelect,
        allow_insert: form.allowInsert,
        allow_update: form.allowUpdate,
        allow_delete: form.allowDelete,
        allow_ddl: form.allowDdl,
        allow_multi: form.allowMulti,
      },
      timeout: form.timeout,
      max_rows: form.maxRows,
      confirm_required: form.confirmRequired,
    };
    await updateTool(props.serviceId, "EXECUTE_SQL", {
      tool_type: "EXECUTE_SQL",
      enabled: props.tool.enabled,
      config: JSON.stringify(config),
    });
    ElMessage.success(t("common.saveSuccess"));
    emit("saved");
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};

const toggleOp = (key: string) => {
  (form as any)[key] = !(form as any)[key];
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="t('mcpService.tool.configExecuteSql')"
    width="480px"
    :close-on-click-modal="false"
  >
    <div class="flex flex-col gap-4">
      <div>
        <label class="config-label">{{ t("mcpService.tool.allowedOps") }}</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="op in ops"
            :key="op.key"
            type="button"
            class="perm-pill"
            :class="{ active: (form as any)[op.key] }"
            @click="toggleOp(op.key)"
          >
            {{ op.label }}
          </button>
        </div>
        <p class="warning-text">
          <el-icon><WarningFilled /></el-icon>
          {{ t("mcpService.tool.sqlRiskWarning") }}
        </p>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div class="flex flex-col gap-1">
          <label class="config-label">{{ t("mcpService.tool.maxRows") }}</label>
          <el-input-number v-model="form.maxRows" :min="1" :max="100000" size="small" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="config-label">{{ t("mcpService.tool.timeout") }} (s)</label>
          <el-input-number v-model="form.timeout" :min="1" :max="300" size="small" />
        </div>
      </div>

      <div class="pt-1">
        <el-checkbox v-model="form.confirmRequired">
          {{ t("mcpService.tool.confirmRequired") }}
        </el-checkbox>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">{{ t("common.save") }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.config-label { display: block; font-size: 13px; font-weight: 600; color: var(--ep-text-color-primary); margin-bottom: 6px; }
.perm-pill {
  padding: 4px 12px; border: 2px solid var(--ep-border-color); border-radius: 6px;
  font-size: 12px; font-weight: 500; cursor: pointer; background: var(--ep-bg-color);
  color: var(--ep-text-color-regular); transition: all 0.15s;
}
.perm-pill.active {
  background: var(--ep-color-primary-light-9);
  border-color: var(--ep-color-primary);
  color: var(--ep-color-primary);
  font-weight: 600;
}
.perm-pill:hover { border-color: var(--ep-color-primary); }
.warning-text { display: flex; align-items: flex-start; gap: 4px; margin-top: 8px; font-size: 12px; color: var(--ep-color-warning); }
</style>
