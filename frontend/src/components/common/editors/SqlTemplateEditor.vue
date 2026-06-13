<!-- frontend/src/components/common/editors/SqlTemplateEditor.vue -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { sql } from "@codemirror/lang-sql";
import { autocompletion } from "@codemirror/autocomplete";
import { bracketMatching, syntaxHighlighting } from "@codemirror/language";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { templateDecorations } from "~/utils/codemirror/template-decorations";
import { templateCompletions } from "~/utils/codemirror/completions/template-completions";
import { templateAutoClose } from "~/utils/codemirror/completions/template-auto-close";
import { datiSqlHighlight } from "~/utils/codemirror/sql-highlight";

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
}>();

const cm = useCodeMirror({
  modelValue: useVModel(props, "modelValue", emit),
  extensions: [
    sql(),
    syntaxHighlighting(datiSqlHighlight),
    autocompletion({ override: [templateCompletions()] }),
    templateAutoClose(),
    templateDecorations(),
    bracketMatching(),
  ],
  placeholder: `SELECT * FROM {{{table}}} {{#where}} {{#if id}}AND id = {{id}}{{/if}} {{/where}}`,
});
</script>

<template>
  <div :ref="cm.containerRef" class="cm-editor-wrapper"></div>
</template>
