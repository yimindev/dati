<!-- frontend/src/components/common/editors/SqlEditor.vue — minimal SQL editor for free-form input -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { sql } from "@codemirror/lang-sql";
import { syntaxHighlighting } from "@codemirror/language";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { datiSqlHighlight } from "~/utils/codemirror/sql-highlight";

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
}>();

const cm = useCodeMirror({
  modelValue: useVModel(props, "modelValue", emit),
  extensions: [sql(), syntaxHighlighting(datiSqlHighlight)],
});
</script>

<template>
  <div :ref="cm.containerRef" class="cm-editor-wrapper sql-editor"></div>
</template>

<style scoped>
.sql-editor :deep(.cm-editor) { min-height: 220px; }
</style>
