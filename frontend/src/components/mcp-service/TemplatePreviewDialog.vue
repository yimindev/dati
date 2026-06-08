<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { CopyDocument } from "@element-plus/icons-vue";
import { previewTemplate } from "~/api/template-preview";

const { t } = useI18n();

const props = defineProps<{
  modelValue: boolean;
  template: string;
  mode: "TEXT" | "SQL";
  parameters: { name: string; type?: string; description?: string }[];
}>();
const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
}>();

const previewValues = reactive<Record<string, any>>({});
const result = ref("");
const loading = ref(false);

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      Object.keys(previewValues).forEach((k) => delete previewValues[k]);
      props.parameters.forEach((p) => {
        if (p.name) previewValues[p.name] = "";
      });
      result.value = "";
    }
  },
);

const doPreview = async () => {
  const hasValue = Object.values(previewValues).some((v) => v !== "" && v !== null);
  if (!hasValue) {
    ElMessage.warning(t("mcpService.tool.previewEmptyValues"));
    return;
  }

  const parsed: Record<string, any> = {};
  for (const [k, v] of Object.entries(previewValues)) {
    if (v === "" || v === null) {
      parsed[k] = null;
      continue;
    }
    if (props.mode === "SQL") {
      const param = props.parameters.find((p) => p.name === k);
      if (param?.type === "Number") parsed[k] = Number(v);
      else if (param?.type === "Boolean") parsed[k] = v === "true";
      else if (param?.type === "Array") parsed[k] = String(v).split(",").map((s) => s.trim()).filter(Boolean);
      else parsed[k] = v;
    } else {
      parsed[k] = v;
    }
  }

  loading.value = true;
  try {
    const resp = await previewTemplate({ mode: props.mode, template: props.template, values: parsed });
    result.value = resp.rendered;
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  } finally {
    loading.value = false;
  }
};

const copyResult = () => {
  navigator.clipboard.writeText(result.value);
  ElMessage.success(t("mcpService.copySuccess"));
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="t('mcpService.tool.previewTitle')"
    width="560px"
    class="max-[620px]:!w-[92vw]"
    :close-on-click-modal="false"
    append-to-body
  >
    <div class="flex flex-col gap-5">
      <div>
        <div class="p-3 px-4 bg-[var(--ep-fill-color-light)] rounded-md font-mono text-[13px] leading-relaxed whitespace-pre-wrap break-all text-[var(--ep-text-color-regular)] max-h-40 overflow-y-auto">{{ template }}</div>
      </div>

      <div v-if="parameters.length > 0">
        <label class="block text-[13px] font-semibold text-[var(--ep-text-color-primary)] mb-2">{{ t("mcpService.tool.parameters") }}</label>
        <div class="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <div v-for="p in parameters" :key="p.name" class="flex flex-col gap-1">
            <span class="text-[13px] font-medium text-[var(--ep-text-color-secondary)]">{{ p.name }}</span>
            <el-input
              v-model="previewValues[p.name]"
              size="small"
              :aria-label="p.name"
              :placeholder="p.description || t('mcpService.tool.previewParamPlaceholder')"
            />
          </div>
        </div>
      </div>

      <div v-if="result" class="border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden" aria-live="polite">
        <div class="flex justify-between items-center px-4 py-2 border-b border-[var(--ep-border-color-lighter)] text-[13px] font-semibold text-[var(--ep-text-color-secondary)]">
          <span>{{ t("mcpService.tool.previewTextResult") }}</span>
          <el-button size="small" text :icon="CopyDocument" @click="copyResult">
            {{ t("common.copy") }}
          </el-button>
        </div>
        <div class="bg-[var(--ep-fill-color)] border border-[var(--ep-border-color)] rounded-md px-4 py-3"><pre class="m-0 font-mono text-[13px] leading-relaxed whitespace-pre-wrap break-all text-[var(--ep-text-color-regular)]">{{ result }}</pre></div>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t("common.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="doPreview">
        {{ t("mcpService.tool.previewRun") }}
      </el-button>
    </template>
  </el-dialog>
</template>

