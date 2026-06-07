<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { previewTemplate } from "~/api/template-preview";
import type { ToolParameter } from "~/api/mcp-tool";
import type { PromptParameter } from "~/api/mcp-prompt";

const { t } = useI18n();

const props = defineProps<{
  modelValue: boolean;
  mode: "TEXT" | "SQL";
  template: string;
  parameters: (ToolParameter | PromptParameter)[];
}>();
const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
}>();

const previewing = ref(false);
const rendered = ref("");

const values = reactive<Record<string, any>>({});

const initValues = () => {
  for (const key of Object.keys(values)) delete values[key];
  for (const p of props.parameters) {
    if (p.name) values[p.name] = "";
  }
  rendered.value = "";
};
initValues();

const hasValues = () => Object.values(values).some((v) => v !== "" && v !== null);

const handlePreview = async () => {
  if (!hasValues()) {
    ElMessage.warning(t("mcpService.tool.previewEmptyValues"));
    return;
  }

  const parsedValues: Record<string, any> = {};
  for (const [key, val] of Object.entries(values)) {
    if (val === "" || val === null) {
      parsedValues[key] = null;
    } else if (props.mode === "SQL") {
      const param = (props.parameters as ToolParameter[]).find((p) => p.name === key);
      if (param?.type === "Number") {
        parsedValues[key] = Number(val);
      } else if (param?.type === "Boolean") {
        parsedValues[key] = val === "true";
      } else if (param?.type === "Array") {
        parsedValues[key] = String(val).split(",").map((s) => s.trim()).filter(Boolean);
      } else {
        parsedValues[key] = val;
      }
    } else {
      parsedValues[key] = val;
    }
  }

  previewing.value = true;
  try {
    const resp = await previewTemplate({
      mode: props.mode,
      template: props.template,
      values: parsedValues,
    });
    rendered.value = resp.rendered;
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  } finally {
    previewing.value = false;
  }
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="t('mcpService.tool.previewTitle')"
    width="640px"
    @open="initValues"
  >
    <!-- Parameter Inputs -->
    <div class="flex flex-col gap-3 mb-4">
      <div
        v-for="p in parameters"
        :key="p.name"
        class="flex items-center gap-2"
      >
        <label class="w-32 text-sm font-medium shrink-0">{{ p.name }}</label>
        <el-input
          v-model="values[p.name]"
          :placeholder="t('mcpService.tool.previewParamPlaceholder')"
          size="small"
          class="flex-1"
        />
        <span
          v-if="p.description"
          class="text-xs text-[var(--ep-text-color-placeholder)] w-24 truncate"
        >{{ p.description }}</span>
      </div>
    </div>

    <div class="flex justify-end mb-4">
      <el-button type="primary" :loading="previewing" @click="handlePreview">
        {{ t("mcpService.tool.previewRender") }}
      </el-button>
    </div>

    <!-- Result -->
    <div v-if="rendered" class="preview-result">
      <div class="text-xs font-semibold mb-2 text-[var(--ep-text-color-secondary)]">
        {{ mode === "SQL" ? t("mcpService.tool.previewSqlResult") : t("mcpService.tool.previewTextResult") }}
      </div>
      <pre class="result-code">{{ rendered }}</pre>
    </div>
  </el-dialog>
</template>

<style scoped>
.preview-result {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  background: var(--ep-fill-color-lighter);
}
.result-code {
  margin: 0;
  font-family: "Consolas", "Monaco", monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ep-text-color-primary);
}
</style>
