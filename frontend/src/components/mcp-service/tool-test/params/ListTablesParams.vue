<script setup lang="ts">
import { useI18n } from "vue-i18n";

const { t } = useI18n();

defineExpose({
  getArgs: () => ({}),
});
</script>

<template>
  <div class="text-[13px] leading-5 text-[var(--ep-text-color-secondary)]">
    {{ t("mcpService.toolTest.noParams") }}
  </div>
</template>
