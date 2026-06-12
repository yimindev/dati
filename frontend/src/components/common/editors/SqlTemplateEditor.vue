<!-- frontend/src/components/common/editors/SqlTemplateEditor.vue -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { sql } from "@codemirror/lang-sql";
import { bracketMatching, syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language";
import { EditorView } from "@codemirror/view";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { templateDecorations } from "~/utils/codemirror/template-decorations";

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
    syntaxHighlighting(defaultHighlightStyle),
    templateDecorations(),
    bracketMatching(),
    EditorView.lineWrapping,
  ],
  placeholder: `SELECT * FROM {{{table}}} {{#where}} {{#if id}}AND id = {{id}}{{/if}} {{/where}}`,
});
</script>

<template>
  <div :ref="cm.containerRef" class="cm-editor-wrapper"></div>
</template>
