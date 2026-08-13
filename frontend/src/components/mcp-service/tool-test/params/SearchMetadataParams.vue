<script setup lang="ts">
import { reactive } from "vue";
import { useI18n } from "vue-i18n";
import { stripEmpty } from "~/utils/stripEmpty";

const { t } = useI18n();

const form = reactive({ keywords: [] as string[] });

defineExpose({
  getArgs: () => stripEmpty({ ...form }),
});
</script>

<template>
  <el-form label-position="top" size="small">
    <el-form-item :label="t('mcpService.toolTest.keywords')">
      <el-input-tag
        v-model="form.keywords"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.keywordsPlaceholder')"
      />
    </el-form-item>
  </el-form>
</template>
