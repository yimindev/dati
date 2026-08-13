<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { FormInstance } from "element-plus";
import type { McpToolVO } from "~/api/mcp-tool";
import { stripEmpty } from "~/utils/stripEmpty";

const props = defineProps<{
  tool: McpToolVO;
  dataSources: { id: string; name: string }[];
}>();

const { t } = useI18n();

const paramDefs = computed(() => (props.tool?.config as any)?.parameters || []);

const form = reactive<Record<string, any>>({});
(paramDefs.value || []).forEach((p: any) => {
  form[p.name] = p.default_value ?? undefined;
});

const dsNameMap = computed(() => {
  const map: Record<string, string> = {};
  props.dataSources.forEach((ds) => {
    map[ds.id] = ds.name;
  });
  return map;
});

const formRef = ref<FormInstance>();

const formRules = computed(() => {
  const rules: Record<string, any> = {};
  for (const p of paramDefs.value) {
    if (p.required) {
      rules[p.name] = [
        {
          required: true,
          message: t("mcpService.toolTest.requiredHint", { name: p.name }),
          trigger: ["blur", "change"],
        },
      ];
    }
  }
  return rules;
});

defineExpose({
  validate: () => formRef.value?.validate() ?? Promise.resolve(true),
  getArgs: () => stripEmpty({ ...form }),
});
</script>

<template>
  <div
    v-if="(tool.config as any)?.data_source_id"
    class="ds-readonly flex items-center gap-2 py-2.5 px-3.5 bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)] rounded-lg mb-5"
  >
    <span class="text-xs text-[var(--ep-text-color-secondary)]">{{
      t("common.dataSource")
    }}</span>
    <span class="text-[13px] font-semibold text-[var(--ep-text-color-primary)]">{{
      dsNameMap[(tool.config as any).data_source_id] ||
      (tool.config as any).data_source_id
    }}</span>
  </div>
  <div
    v-if="paramDefs.length === 0"
    class="text-sm text-[var(--ep-text-color-placeholder)]"
  >
    {{ t("mcpService.toolTest.noParams") }}
  </div>
  <el-form
    v-else
    ref="formRef"
    :model="form"
    :rules="formRules"
    label-position="top"
  >
    <el-form-item
      v-for="p in paramDefs"
      :key="p.name"
      :label="p.name"
      :required="p.required"
      :prop="p.name"
    >
      <ParameterInput :parameter="p" v-model="form[p.name]" />
    </el-form-item>
  </el-form>
</template>
