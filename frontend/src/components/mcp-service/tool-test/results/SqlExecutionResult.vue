<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { CircleCheckFilled, CircleCloseFilled } from "@element-plus/icons-vue";
import type {
  SqlExecution,
  SelectResult,
  WriteResult,
} from "~/api/mcp-tool-test";

defineProps<{ data: SqlExecution }>();

const { t } = useI18n();
</script>

<template>
  <div class="output-block">
    <div class="output-block-title">
      {{ t("mcpService.toolTest.executedSql") }}
    </div>
    <pre class="output-block-body">{{ data.executed_sql }}</pre>
  </div>

  <div v-if="data.bindings?.length" class="output-block">
    <div class="output-block-title">
      {{ t("mcpService.toolTest.bindings") }}
    </div>
    <div class="output-block-body flex flex-wrap gap-x-6 gap-y-1">
      <span v-for="(val, i) in data.bindings" :key="i" class="binding-item">
        ?{{ i + 1 }} = <code>{{ val }}</code>
      </span>
    </div>
  </div>

  <template v-for="(r, i) in data.results" :key="i">
    <div
      class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3"
      :class="{ 'result-failed': !r.success }"
    >
      <!-- Card header -->
      <div
        class="result-card-header flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-2 px-3"
        :class="{ 'header-failed': !r.success }"
      >
        <template v-if="r.success">
          <el-icon class="text-[var(--ep-color-success)]"
            ><CircleCheckFilled
          /></el-icon>
        </template>
        <template v-else>
          <el-icon class="text-[var(--ep-color-danger)]"
            ><CircleCloseFilled
          /></el-icon>
        </template>
        <span>{{
          t(
            r.type === "SELECT"
              ? "mcpService.toolTest.queryResult"
              : "mcpService.toolTest.writeResult",
            { n: i + 1 },
          )
        }}</span>
        <span v-if="r.success && r.type === 'SELECT'" class="ml-auto font-normal">
          {{
            t("mcpService.toolTest.totalRows", {
              total: (r as SelectResult).total_rows,
            })
          }}
        </span>
        <span v-if="r.success && r.type === 'WRITE'" class="ml-auto font-normal">
          {{
            t("mcpService.toolTest.affectedRows", {
              count: (r as WriteResult).affected_rows,
            })
          }}
        </span>
      </div>

      <!-- Card body -->
      <div class="result-card-body p-0">
        <!-- SELECT success -->
        <template v-if="r.success && r.type === 'SELECT'">
          <el-table
            :data="(r as SelectResult).rows"
            border
            size="small"
            max-height="360"
            stripe
          >
            <el-table-column
              v-for="(col, ci) in (r as SelectResult).columns"
              :key="ci"
              :prop="String(ci)"
              :label="col"
            />
          </el-table>
        </template>
        <!-- WRITE success -->
        <template v-else-if="r.success && r.type === 'WRITE'">
          <div
            class="write-result flex items-center gap-2.5 py-3.5 px-4 text-[13px]"
          >
            {{
              t("mcpService.toolTest.affectedRows", {
                count: (r as WriteResult).affected_rows,
              })
            }}
          </div>
        </template>
        <!-- Failed -->
        <template v-else>
          <div
            class="result-error py-3 px-4 text-[13px] text-[var(--ep-color-danger)] font-mono bg-[var(--ep-color-danger-light-9)]"
          >
            {{ (r as any).error_message }}
          </div>
        </template>
      </div>
    </div>
  </template>

  <div
    v-if="data.results.length === 0"
    class="text-sm text-[var(--ep-text-color-placeholder)] text-center py-4"
  >
    {{ t("mcpService.toolTest.emptyResult") }}
  </div>
</template>

<style scoped>
.output-block {
  margin-bottom: 12px;
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.output-block-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--ep-text-color-secondary);
  background: var(--ep-fill-color);
  padding: 6px 14px;
  border-bottom: 1px solid var(--ep-border-color-lighter);
}
.output-block-body {
  background: var(--ep-fill-color-lighter);
  padding: 10px 14px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ep-text-color-primary);
  line-height: 1.6;
  margin: 0;
}
.binding-item code {
  font-family: monospace;
  color: var(--ep-color-primary);
  background-color: transparent;
  padding: 0;
  border-radius: 0;
}

/* ── Result card state variants (requires dynamic :class) ── */
.result-failed {
  border-color: var(--ep-color-danger-light-7);
}
.header-failed {
  background: var(--ep-color-danger-light-9);
  color: var(--ep-color-danger);
}
</style>
