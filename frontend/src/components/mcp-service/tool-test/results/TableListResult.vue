<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { Coin } from "@element-plus/icons-vue";
import type { TableListData } from "~/api/mcp-tool-test";

const props = defineProps<{ data: TableListData }>();

const { t } = useI18n();

const totalTables = computed(() =>
  props.data.data_sources.reduce((sum, ds) => sum + (ds.tables?.length ?? 0), 0),
);

const dbTypeLabel = (type: string) =>
  ({
    POSTGRESQL: "PostgreSQL",
    MYSQL: "MySQL",
    CLICKHOUSE: "ClickHouse",
    ORACLE: "Oracle",
    SQLSERVER: "SQL Server",
    H2: "H2",
    MARIADB: "MariaDB",
    DUCKDB: "DuckDB",
    SQLITE: "SQLite",
    TRINO: "Trino",
  })[type] || type;

const dbTypeColor = (type: string) =>
  ({
    POSTGRESQL: "var(--ep-color-primary)",
    MYSQL: "var(--ep-color-warning)",
    CLICKHOUSE: "var(--ep-color-success)",
    ORACLE: "var(--ep-color-danger)",
  })[type] || "var(--ep-color-info)";
</script>

<template>
  <div>
    <div class="result-section-header mb-3">
      <el-icon class="text-[var(--ep-color-primary)]"><Coin /></el-icon>
      <span>{{ t("mcpService.toolTest.listTablesTitle") }}</span>
      <el-tag size="small" round class="ml-1">{{ totalTables }} {{ t("mcpService.toolTest.tables") }}</el-tag>
    </div>

    <template v-for="ds in data.data_sources" :key="ds.id">
      <div class="ds-card border border-[var(--ep-border-color-lighter)] rounded-lg mb-3 overflow-hidden">
        <div class="ds-header flex items-center justify-between bg-[var(--ep-fill-color-lighter)] px-4 py-3">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="font-semibold text-sm">{{ ds.name }}</span>
            <el-tag
              v-if="ds.db_type"
              size="small"
              :color="dbTypeColor(ds.db_type)"
              effect="dark"
            >
              {{ dbTypeLabel(ds.db_type) }}
            </el-tag>
          </div>
          <el-tag size="small" round>{{ ds.tables.length }} {{ t("mcpService.toolTest.tables") }}</el-tag>
        </div>
        <div v-if="ds.description" class="text-xs text-[var(--ep-text-color-secondary)] px-4 py-1.5 bg-[var(--ep-bg-color)]">
          {{ ds.description }}
        </div>
        <div class="ds-body px-3 py-2 flex flex-col gap-2">
          <div
            v-for="tbl in ds.tables"
            :key="tbl.table"
            class="flex items-start gap-2 border border-[var(--ep-border-color-lighter)] rounded-md px-3 py-2"
          >
            <code class="table-name shrink-0">
              <span class="text-[var(--ep-text-color-placeholder)]">{{ tbl.schema ? tbl.schema + "." : "" }}</span>{{ tbl.table }}
            </code>
            <p v-if="tbl.description" class="text-xs text-[var(--ep-text-color-secondary)] leading-5 flex-1 min-w-0">
              {{ tbl.description }}
            </p>
            <div v-if="tbl.aliases?.length" class="flex items-center gap-1 shrink-0 flex-wrap">
              <el-tag v-for="a in tbl.aliases" :key="a" size="small">{{ a }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty
      v-if="!data.data_sources?.length"
      :description="t('mcpService.toolTest.emptyTableList')"
    />
  </div>
</template>

<style scoped>
.table-name {
  font-size: 12px;
  color: var(--ep-text-color-primary);
  background: var(--ep-fill-color);
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
