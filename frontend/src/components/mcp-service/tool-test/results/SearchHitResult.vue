<script setup lang="ts">
import { useI18n } from "vue-i18n";
import {
  CircleCheckFilled,
  Menu as IconMenu,
  Coin,
} from "@element-plus/icons-vue";
import type { SearchHit } from "~/api/mcp-tool-test";

defineProps<{ data: SearchHit }>();

const { t } = useI18n();

const dbTypeLabel = (t: string) =>
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
  })[t] || t;

const dbTypeColor = (t: string) =>
  ({
    POSTGRESQL: "var(--ep-color-primary)",
    MYSQL: "var(--ep-color-warning)",
    CLICKHOUSE: "var(--ep-color-success)",
    ORACLE: "var(--ep-color-danger)",
  })[t] || "var(--ep-color-info)";
</script>

<template>
  <div v-if="data.terms?.length" class="mb-6">
    <div class="result-section-header">
      <el-icon class="text-[var(--ep-color-primary)]"><IconMenu /></el-icon>
      <span
        >{{ t("mcpService.toolTest.matchTerms") }} ({{
          data.terms.length
        }})</span
      >
    </div>
    <div
      v-for="t in data.terms"
      :key="`${t.name}-${t.subject_name}`"
      class="p-3 rounded mb-2 bg-[var(--ep-fill-color-lighter)]"
    >
      <div class="flex items-center gap-2">
        <span class="font-semibold text-sm">{{ t.name }}</span>
        <span class="text-xs text-[var(--ep-text-color-placeholder)]">
          {{ t.subject_name }}</span
        >
      </div>
      <p
        v-if="t.description"
        class="text-sm mt-1 text-[var(--ep-text-color-secondary)]"
      >
        {{ t.description }}
      </p>
    </div>
  </div>
  <div class="mb-1">
    <div class="result-section-header">
      <el-icon class="text-[var(--ep-color-primary)]"><Coin /></el-icon>
      <span>{{ t("mcpService.toolTest.matchSources") }}</span>
    </div>
  </div>
  <div v-if="data.data_sources">
    <template v-for="ds in data.data_sources" :key="ds.id">
      <div
        class="ds-card border border-[var(--ep-border-color-lighter)] rounded-lg mb-3 overflow-hidden"
      >
        <div
          class="ds-header flex items-center justify-between bg-[var(--ep-fill-color-lighter)] px-4 py-3"
        >
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
          <el-tag size="small" round
            >{{ ds.tables.length }}
            {{ t("mcpService.toolTest.tables") }}</el-tag
          >
        </div>
        <div
          v-if="ds.description"
          class="text-xs text-[var(--ep-text-color-secondary)] px-4 py-1.5 bg-[var(--ep-bg-color)]"
        >
          {{ ds.description }}
        </div>
        <div class="ds-body px-3 pb-3 pt-2 flex flex-col gap-2">
          <template v-for="tbl in ds.tables" :key="tbl.table">
            <div
              class="border border-[var(--ep-border-color-lighter)] rounded-md overflow-hidden"
            >
              <div
                class="flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-1.5 px-3"
              >
                <el-icon class="text-[var(--ep-color-success)]"
                  ><CircleCheckFilled
                /></el-icon>
                <span
                  ><span
                    class="text-[var(--ep-text-color-placeholder)] font-normal"
                    >{{ tbl.schema ? tbl.schema + "." : "" }}</span
                  >{{ tbl.table }}</span
                >
                <span
                  v-if="tbl.description"
                  class="font-normal text-[var(--ep-text-color-placeholder)] ml-1"
                >
                  — {{ tbl.description }}</span
                >
              </div>
              <el-table
                v-if="tbl.columns?.length"
                :data="tbl.columns"
                border
                size="small"
              >
                <el-table-column prop="name" label="Column" width="140" />
                <el-table-column prop="type" label="Type" width="120" />
                <el-table-column prop="comment" label="Comment" min-width="120" />
                <el-table-column label="Aliases" width="140">
                  <template #default="{ row }">
                    <el-tag
                      v-for="a in row.aliases"
                      :key="a"
                      size="small"
                      class="mr-1"
                      >{{ a }}</el-tag
                    >
                  </template>
                </el-table-column>
                <el-table-column label="Sample Values" min-width="160">
                  <template #default="{ row }">
                    <el-tag
                      v-for="v in row.sample_values"
                      :key="v"
                      size="small"
                      type="info"
                      class="mr-1"
                      >{{ v }}</el-tag
                    >
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>
      </div>
    </template>
  </div>
  <el-empty
    v-if="!data.data_sources?.length && !data.terms?.length"
    :description="t('mcpService.toolTest.emptyResult')"
  />
</template>

<style scoped>
/* ── Search hit layout ── */
.result-section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 650;
  color: var(--ep-text-color-primary);
  margin-bottom: 12px;
}
.ds-card {
  background: var(--ep-bg-color);
}
.ds-header {
  border-bottom: 1px solid var(--ep-border-color-lighter);
}
.ds-body {
  background: var(--ep-bg-color);
}
</style>
