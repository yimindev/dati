<!-- frontend/src/components/common/editors/PromptTemplateEditor.vue -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { EditorView } from "@codemirror/view";
import { autocompletion } from "@codemirror/autocomplete";import { EditorState } from "@codemirror/state";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { templateDecorations } from "~/utils/codemirror/template-decorations";
import { templateCompletions } from "~/utils/codemirror/completions/template-completions";
import { templateAutoClose } from "~/utils/codemirror/completions/template-auto-close";
import {bracketMatching} from "@codemirror/language";

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
}>();

const cm = useCodeMirror({
  modelValue: useVModel(props, "modelValue", emit),
  extensions: [
    autocompletion(),
    EditorState.languageData.of(() => [{ autocomplete: templateCompletions() }]),
    templateAutoClose(),
    templateDecorations(),
    bracketMatching(),
    EditorView.lineWrapping,
  ],
  placeholder: "请分析 {{table_name}} 表的数据...",
});
</script>

<template>
  <div :ref="cm.containerRef" class="cm-editor-wrapper"></div>
</template>
