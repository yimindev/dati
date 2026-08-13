<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { useI18n } from "vue-i18n";
import { Plus, Delete } from "@element-plus/icons-vue";
import { useTablePicker } from "~/composables/useTablePicker";

defineProps<{ dataSources: { id: string; name: string }[] }>();

const { t } = useI18n();

const { tableLoading, ensureTablesLoaded, schemaOptionsFor, tableOptionsFor } =
  useTablePicker();

const form = reactive<{ tables: Record<string, any>[] }>({
  tables: [{ data_source_id: "", schema: "", table: "" }],
});

const canAddRow = computed(() => {
  const last = form.tables[form.tables.length - 1];
  return !!last && !!last.data_source_id;
});

const addRow = () => {
  if (!canAddRow.value) return;
  form.tables.push({ data_source_id: "", schema: "", table: "" });
};

const removeRow = (i: number) => {
  if (form.tables.length > 1) {
    form.tables.splice(i, 1);
  }
};

watch(
  () => form.tables.map((e) => e.data_source_id),
  async (ids) => {
    await Promise.all(ids.map((id: string) => ensureTablesLoaded(id)));
  },
  { deep: true },
);

defineExpose({
  getArgs: () => ({
    tables: form.tables.map((e) => ({
      data_source_id: e.data_source_id,
      schema: e.schema || null,
      table: e.table,
    })),
  }),
});
</script>

<template>
  <div class="flex flex-col gap-2.5">
    <div
      v-for="(entry, i) in form.tables"
      :key="i"
      class="flex flex-col gap-2 border border-[var(--ep-border-color-lighter)] rounded-lg p-2.5 bg-[var(--ep-fill-color-blank)] transition-colors hover:border-[var(--ep-border-color)]"
    >
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold text-[var(--ep-text-color-secondary)]">
          {{ t("mcpService.toolTest.tableRow", { n: i + 1 }) }}
        </span>
        <el-tooltip :content="t('common.delete')" placement="top">
          <el-button
            size="small"
            text
            type="danger"
            circle
            :icon="Delete"
            :aria-label="t('common.delete')"
            @click="removeRow(i)"
            :disabled="form.tables.length <= 1"
          />
        </el-tooltip>
      </div>

      <el-select
        v-model="entry.data_source_id"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.dataSourcePlaceholder')"
        filterable
        @change="entry.schema = ''; entry.table = ''"
      >
        <el-option
          v-for="ds in dataSources"
          :key="ds.id"
          :label="ds.name"
          :value="ds.id"
        />
      </el-select>

      <div class="flex gap-2">
        <el-select
          v-model="entry.schema"
          size="small"
          :placeholder="t('mcpService.toolTest.schemaPlaceholder')"
          filterable
          clearable
          class="flex-1 min-w-0"
          :loading="tableLoading"
          :disabled="!entry.data_source_id"
          @change="entry.table = ''"
        >
          <el-option
            v-for="s in schemaOptionsFor(entry)"
            :key="s"
            :label="s"
            :value="s"
          />
        </el-select>

        <el-select
          v-model="entry.table"
          size="small"
          :placeholder="t('mcpService.toolTest.tablePlaceholder')"
          filterable
          clearable
          class="flex-1 min-w-0"
          :disabled="!entry.data_source_id"
        >
          <el-option
            v-for="t in tableOptionsFor(entry)"
            :key="t.name"
            :label="t.name"
            :value="t.name"
          />
        </el-select>
      </div>
    </div>

    <el-button
      size="small"
      class="w-full !border-dashed"
      :icon="Plus"
      :disabled="!canAddRow"
      @click="addRow"
    >
      {{ t("mcpService.toolTest.addRow") }}
    </el-button>
  </div>
</template>
