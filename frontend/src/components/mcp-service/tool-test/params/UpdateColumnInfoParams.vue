<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { useI18n } from "vue-i18n";
import { Plus, Delete } from "@element-plus/icons-vue";
import { useTablePicker } from "~/composables/useTablePicker";
import { stripEmpty } from "~/utils/stripEmpty";

defineProps<{ dataSources: { id: string; name: string }[] }>();

const { t } = useI18n();

const {
  tableLoading,
  columnLoading,
  ensureTablesLoaded,
  schemaOptionsFor,
  tableOptionsFor,
  columnOptionsFor,
  onDsPicked,
  onTablePickedForColumn,
  onColumnPicked,
} = useTablePicker();

const form = reactive<{ tables: Record<string, any>[] }>({
  tables: [
    {
      data_source_id: "",
      schema: "",
      table: "",
      column: "",
      description: "",
      aliases: [],
    },
  ],
});

const canAddRow = computed(() => {
  const last = form.tables[form.tables.length - 1];
  return !!last && !!last.data_source_id && !!last.table && !!last.column;
});

const addRow = () => {
  if (!canAddRow.value) return;
  form.tables.push({
    data_source_id: "",
    schema: "",
    table: "",
    column: "",
    description: "",
    aliases: [],
  });
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
  getArgs: () => ({ columns: form.tables.map(stripEmpty) }),
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
          {{ t("mcpService.toolTest.columnRow", { n: i + 1 }) }}
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
        @change="onDsPicked(entry)"
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
          :disabled="!entry.data_source_id"
          :loading="tableLoading"
          @change="entry.table = ''; entry.column = ''; entry.description = ''; entry.aliases = []"
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
          :loading="tableLoading"
          @change="onTablePickedForColumn(entry)"
        >
          <el-option
            v-for="t in tableOptionsFor(entry)"
            :key="t.name"
            :label="t.name"
            :value="t.name"
          />
        </el-select>
      </div>

      <el-select
        v-model="entry.column"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.columnPlaceholder')"
        filterable
        clearable
        :disabled="!entry.data_source_id || !entry.table"
        :loading="columnLoading"
        @change="onColumnPicked(entry)"
      >
        <el-option
          v-for="c in columnOptionsFor(entry)"
          :key="c"
          :label="c"
          :value="c"
        />
      </el-select>

      <el-input
        v-model="entry.description"
        size="small"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        :placeholder="t('mcpService.toolTest.descriptionPlaceholder')"
      />
      <el-input-tag
        v-model="entry.aliases"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.aliasesPlaceholder')"
      />
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
