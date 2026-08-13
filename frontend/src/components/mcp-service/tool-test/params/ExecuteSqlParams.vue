<script setup lang="ts">
import { reactive } from "vue";
import { useI18n } from "vue-i18n";
import { stripEmpty } from "~/utils/stripEmpty";

defineProps<{ dataSources: { id: string; name: string }[] }>();

const { t } = useI18n();

const form = reactive({ data_source_id: "", sql: "" });

defineExpose({
  getArgs: () => stripEmpty({ ...form }),
});
</script>

<template>
  <el-form label-position="top" size="small">
    <el-form-item :label="t('common.dataSource')" required>
      <el-select
        v-model="form.data_source_id"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.dataSourcePlaceholder')"
        filterable
      >
        <el-option
          v-for="ds in dataSources"
          :key="ds.id"
          :label="ds.name"
          :value="ds.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item>
      <SqlEditor v-model="form.sql" label="SQL" required />
    </el-form-item>
  </el-form>
</template>
