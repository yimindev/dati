<!-- frontend/src/components/common/editors/SqlEditor.vue — minimal SQL editor for free-form input -->
<script setup lang="ts">
import { useVModel } from "@vueuse/core";
import { sql } from "@codemirror/lang-sql";
import { FullScreen } from "@element-plus/icons-vue";
import { syntaxHighlighting } from "@codemirror/language";
import { useI18n } from "vue-i18n";
import { useCodeMirror } from "~/composables/useCodeMirror";
import { useEditorFullscreen } from "~/composables/useEditorFullscreen";
import { datiSqlHighlight } from "~/utils/codemirror/sql-highlight";

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
  extensions: [sql(), syntaxHighlighting(datiSqlHighlight)],
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
    <div :ref="cm.containerRef" class="cm-editor-wrapper sql-editor flex-1" />
  </div>
</template>

<style scoped>
.sql-editor :deep(.cm-editor) { min-height: 220px; }
</style>
