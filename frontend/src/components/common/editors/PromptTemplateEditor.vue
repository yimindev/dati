<!-- frontend/src/components/common/editors/PromptTemplateEditor.vue -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { EditorView } from "@codemirror/view";
import { FullScreen } from "@element-plus/icons-vue";
import { autocompletion } from "@codemirror/autocomplete";
import { useI18n } from "vue-i18n";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { useEditorFullscreen } from "~/composables/useEditorFullscreen";
import { templateDecorations } from "~/utils/codemirror/template-decorations";
import { templateCompletions } from "~/utils/codemirror/completions/template-completions";
import { templateAutoClose } from "~/utils/codemirror/completions/template-auto-close";
import {bracketMatching} from "@codemirror/language";

const { t } = useI18n();

const props = defineProps<{
  modelValue: string;
  label?: string;
  required?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", v: string): void;
}>();

const { isFullscreen, toggle } = useEditorFullscreen();

const cm = useCodeMirror({
  modelValue: useVModel(props, "modelValue", emit),
  extensions: [
    // override 方式不依赖 Language 实例（languageData 方式需 @codemirror/language 的 Language 才会被 autocompletion 读取）
    autocompletion({ override: [templateCompletions()] }),
    templateAutoClose(),
    templateDecorations(),
    bracketMatching(),
    EditorView.lineWrapping,
  ],
  placeholder: "请分析 {{table_name}} 表的数据...",
});
</script>

<template>
  <div class="w-full" :class="{ 'cm-editor-fullscreen': isFullscreen }">
    <div class="flex items-center justify-between mb-1">
      <span v-if="label" class="text-sm text-[var(--ep-text-color-primary)]">
        <span v-if="required" class="text-[var(--ep-color-danger)] mr-0.5">*</span>{{ label }}
      </span>
      <span v-else />
      <el-tooltip
        :content="isFullscreen ? t('common.exitFullscreen') : t('common.fullscreen')"
        :show-after="500"
      >
        <el-button
          :icon="isFullscreen ? undefined : FullScreen"
          size="small"
          text
          :aria-label="isFullscreen ? t('common.exitFullscreen') : t('common.fullscreen')"
          @click="toggle"
        >
          <span v-if="isFullscreen" class="icon-[mdi--fullscreen-exit]"></span>
        </el-button>
      </el-tooltip>
    </div>
    <div :ref="cm.containerRef" class="cm-editor-wrapper flex-1" />
  </div>
</template>

<style scoped>
:deep(.cm-editor) { max-height: 400px; }
:deep(.cm-scroller) { overflow-y: auto; }
</style>
