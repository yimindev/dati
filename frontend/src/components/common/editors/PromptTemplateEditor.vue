<!-- frontend/src/components/common/editors/PromptTemplateEditor.vue -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
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
  extensions: [templateDecorations(), EditorView.lineWrapping],
  placeholder: "请分析 {{table_name}} 表的数据...",
});
</script>

<template>
  <div :ref="cm.containerRef" class="cm-editor-wrapper"></div>
</template>
