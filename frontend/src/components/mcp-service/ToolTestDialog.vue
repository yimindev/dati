<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { useI18n } from "vue-i18n";
import { Plus, Delete, CircleCheckFilled, CircleCloseFilled } from "@element-plus/icons-vue";
import type { McpToolVO } from "~/api/mcp-tool";
import type { ToolTestResponse, SqlExecution, TableMetadata, SelectResult, WriteResult } from "~/api/mcp-tool-test";
import { testTool } from "~/api/mcp-tool-test";
import { getDataScope } from "~/api/mcp-service";
import { listTableInfos } from "~/api/tableinfo";

const { t } = useI18n();

const props = defineProps<{
  visible: boolean;
  serviceId: string;
  tool: McpToolVO;
}>();
const emit = defineEmits<{ (e: "update:visible", v: boolean): void }>();

const loading = ref(false);
const response = ref<ToolTestResponse | null>(null);

// --- Default form values per tool type ---
const defaultForm = (): Record<string, any> => {
  switch (props.tool?.tool_type) {
    case "EXECUTE_SQL":
      return { data_source_id: "", sql: "" };
    case "GET_TABLE_INFO":
      return { data_source_id: "", tables: [{ schema: "", table: "" }] };
    case "PARAMETERIZED_SQL": {
      const params: Record<string, any> = {};
      ((props.tool.config as any)?.parameters || []).forEach((p: any) => {
        params[p.name] = p.default_value ?? undefined;
      });
      return params;
    }
    default:
      return {};
  }
};

const form = reactive<Record<string, any>>(defaultForm());

// Reset on open
watch(() => props.visible, (v) => {
  if (v) {
    const defaults = defaultForm();
    Object.keys(form).forEach(k => delete form[k]);
    Object.assign(form, defaults);
    response.value = null;
    schemaOptions.value = [];
    tableOptions.value = {};
    allTables.value = [];
  }
});

// Parameter definitions
const paramDefs = computed(() => (props.tool?.config as any)?.parameters || []);

// GET_TABLE_INFO table list
const addTableEntry = () => (form.tables as any[])?.push({ schema: "", table: "" });
const removeTableEntry = (i: number) => (form.tables as any[])?.splice(i, 1);

// Schema & table dropdowns for GET_TABLE_INFO (from platform-managed tables)
const allTables = ref<{ schema: string; name: string }[]>([]);
const schemaOptions = ref<string[]>([]);
const tableOptions = ref<Record<number, { name: string }[]>>({});
const tableLoading = ref(false);

watch(() => form.data_source_id, async (dsId) => {
  schemaOptions.value = [];
  tableOptions.value = {};
  if (!dsId) return;
  tableLoading.value = true;
  try {
    const resp = await listTableInfos(dsId, 1, 9999);
    allTables.value = (resp.data || []).map((t: any) => ({ schema: t.schema, name: t.name }));
    schemaOptions.value = [...new Set(allTables.value.map(t => t.schema).filter(Boolean))].sort() as string[];
  } catch { allTables.value = []; }
  finally { tableLoading.value = false; }
});

watch(() => (form.tables as any[])?.map((e: any) => e.schema), (schemas) => {
  if (!schemas) return;
  schemas.forEach((schema: string, i: number) => {
    tableOptions.value[i] = schema
      ? allTables.value.filter(t => t.schema === schema).map(t => ({ name: t.name }))
      : [];
  });
}, { deep: true });

// Data source selector
const dataSources = ref<{ id: string; name: string }[]>([]);
const dsNameMap = computed(() => {
  const map: Record<string, string> = {};
  dataSources.value.forEach(ds => { map[ds.id] = ds.name; });
  return map;
});
watch(() => props.visible, async (v) => {
  if (v) {
    try {
      const resp = await getDataScope(props.serviceId);
      dataSources.value = (resp.resolved_data_sources || []).map((ds: any) => ({
        id: ds.id, name: ds.name,
      }));
    } catch { /* ignore */ }
  }
}, { immediate: true });

// --- Execute ---
const doExecute = async () => {
  loading.value = true;
  response.value = null;
  try {
    const args: Record<string, any> = {};
    if (props.tool?.tool_type === "GET_TABLE_INFO") {
      args.data_source_id = form.data_source_id;
      args.tables = (form.tables as any[])?.map((e: any) => ({
        schema: e.schema || null, table: e.table,
      })) || [];
    } else {
      Object.assign(args, form);
      Object.keys(args).forEach(k => {
        const v = args[k];
        if (v === null || v === undefined || v === '' || (Array.isArray(v) && v.length === 0)) {
          delete args[k];
        }
      });
    }
    response.value = await testTool(props.serviceId, props.tool!.id, { arguments: args });
  } catch (e: any) {
    ElMessage.error(e?.message || t("common.operationFailed"));
  } finally {
    loading.value = false;
  }
};


const formRef = ref<FormInstance>();

const formRules = computed(() => {
  const rules: Record<string, any> = {};
  for (const p of paramDefs.value) {
    if (p.required) {
      rules[p.name] = [{ required: true, message: t('mcpService.toolTest.requiredHint', { name: p.name }), trigger: ['blur', 'change'] }];
    }
  }
  return rules;
});

const handleTestClick = async () => {
  if (props.tool?.tool_type === 'PARAMETERIZED_SQL') {
    try { await formRef.value?.validate(); } catch { return; }
  }
  await doExecute();
};

// --- Error advice ---
const errorAdvice = computed(() => {
  const cat = response.value?.error?.error_category;
  switch (cat) {
    case "PARAM_ERROR": return t("mcpService.toolTest.adviceParamError");
    case "SCOPE_ERROR": return t("mcpService.toolTest.adviceScopeError");
    case "PERMISSION_DENIED": return t("mcpService.toolTest.advicePermissionDenied");
    case "SQL_ERROR": return t("mcpService.toolTest.adviceSqlError");
    case "TIMEOUT": return t("mcpService.toolTest.adviceTimeout");
    default: return "";
  }
});

// --- Result helpers ---
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    :title="t('mcpService.toolTest.title', { name: tool?.title || tool?.name || '' })"
    width="960px"
    :close-on-click-modal="false"
    append-to-body
    class="test-dialog"
  >
    <div class="test-layout flex flex-row h-[520px] relative">
      <!-- ====== Left: Parameter Form ====== -->
      <div class="left-panel w-80 shrink-0 flex flex-col pr-5 border-r border-[var(--ep-border-color-lighter)] overflow-y-auto">
        <h4 class="section-title text-[13px] font-semibold text-[var(--ep-text-color-primary)] mb-3">{{ t("mcpService.toolTest.parameters") }}</h4>

        <!-- EXECUTE_SQL -->
        <template v-if="tool?.tool_type === 'EXECUTE_SQL'">
          <el-form label-position="top">
            <el-form-item :label="t('common.dataSource')" required>
              <el-select v-model="form.data_source_id" class="w-full" placeholder="Select data source">
                <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <SqlEditor v-model="form.sql" label="SQL" required />
            </el-form-item>
          </el-form>
        </template>

        <!-- PARAMETERIZED_SQL -->
        <template v-else-if="tool?.tool_type === 'PARAMETERIZED_SQL'">
          <div v-if="(tool.config as any)?.data_source_id" class="ds-readonly flex items-center gap-2 py-2.5 px-3.5 bg-[var(--ep-fill-color-lighter)] border border-[var(--ep-border-color-lighter)] rounded-lg mb-5">
            <span class="text-xs text-[var(--ep-text-color-secondary)]">{{ t("common.dataSource") }}</span>
            <span class="text-[13px] font-semibold text-[var(--ep-text-color-primary)]">{{ dsNameMap[(tool.config as any).data_source_id] || (tool.config as any).data_source_id }}</span>
          </div>
          <div v-if="paramDefs.length === 0" class="text-sm text-[var(--ep-text-color-placeholder)]">
            {{ t("mcpService.toolTest.noParams") }}
          </div>
          <el-form v-else ref="formRef" :model="form" :rules="formRules" label-position="top">
            <el-form-item v-for="p in paramDefs" :key="p.name" :label="p.name" :required="p.required" :prop="p.name">
              <ParameterInput :parameter="p" v-model="form[p.name]" />
            </el-form-item>
          </el-form>
        </template>

        <!-- GET_TABLE_INFO -->
        <template v-else-if="tool?.tool_type === 'GET_TABLE_INFO'">
          <el-form label-position="top">
            <el-form-item :label="t('common.dataSource')" required>
              <el-select v-model="form.data_source_id" class="w-full" placeholder="Select data source">
                <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="flex flex-col gap-2">
            <div v-for="(entry, i) in (form.tables as any[])" :key="i" class="flex items-center gap-1">
              <el-select v-model="entry.schema" placeholder="schema" filterable clearable
                class="!w-[112px] shrink-0" :loading="tableLoading" @change="entry.table = ''">
                <el-option v-for="s in schemaOptions" :key="s" :label="s" :value="s" />
              </el-select>
              <span class="text-[var(--ep-text-color-placeholder)] font-semibold shrink-0">.</span>
              <div class="flex-1 min-w-0">
                <el-select v-model="entry.table" placeholder="table" filterable clearable
                  :disabled="!entry.schema">
                  <el-option v-for="t in (tableOptions[i] || [])" :key="t.name" :label="t.name" :value="t.name" />
                </el-select>
              </div>
              <el-button size="small" text type="danger" :icon="Delete"
                @click="removeTableEntry(i)" :disabled="(form.tables as any[]).length <= 1" />
            </div>
            <el-button size="small" :icon="Plus" @click="addTableEntry">Add table</el-button>
          </div>
        </template>

        <!-- SEARCH_METADATA -->
        <template v-else-if="tool?.tool_type === 'SEARCH_METADATA'">
          <el-empty :description="t('mcpService.toolTest.comingSoon')" />
        </template>

        <!-- Execute button (bottom of left panel) -->
        <div class="execute-area mt-auto pt-4 border-t border-[var(--ep-border-color-lighter)]">
          <el-button type="primary" :loading="loading" @click="handleTestClick" class="w-full"
            :disabled="tool?.tool_type === 'SEARCH_METADATA'">
            {{ t("mcpService.toolTest.runTest") }}
          </el-button>
        </div>
      </div>

      <!-- ====== Right: Result Area ====== -->
      <div class="right-panel flex-1 pl-5 overflow-y-auto overflow-x-hidden relative min-w-0">
        <!-- Initial empty state -->
        <div v-if="!loading && !response" class="result-empty">
          <el-empty :description="t('mcpService.toolTest.hint')" />
        </div>

        <!-- Loading skeleton -->
        <div v-if="loading" class="result-loading">
          <el-skeleton :rows="10" animated />
        </div>

        <!-- Result -->
        <template v-if="response">
          <div class="result-header flex justify-between items-center text-[13px] font-semibold text-[var(--ep-text-color-primary)] mb-3">
            <span>{{ t("mcpService.toolTest.result") }}</span>
            <span class="text-xs font-normal text-[var(--ep-text-color-placeholder)]">{{ t('mcpService.toolTest.elapsed', { ms: response.execution_time_ms }) }}</span>
          </div>

          <!-- Top-level error -->
          <div v-if="!response.success && response.error" class="error-card">
            <div class="error-header">
              <el-icon><CircleCloseFilled /></el-icon>
              <span class="error-category">{{ response.error.error_category }}</span>
            </div>
            <pre class="error-message">{{ response.error.message }}</pre>
            <div v-if="errorAdvice" class="error-advice">{{ errorAdvice }}</div>
          </div>

          <!-- SQL_EXECUTION -->
          <template v-else-if="response.data?.type === 'SQL_EXECUTION'">
            <div class="output-block">
              <div class="output-block-title">{{ t("mcpService.toolTest.executedSql") }}</div>
              <pre class="output-block-body">{{ (response.data as SqlExecution).executed_sql }}</pre>
            </div>

            <div v-if="(response.data as SqlExecution).bindings?.length" class="output-block">
              <div class="output-block-title">{{ t("mcpService.toolTest.bindings") }}</div>
              <div class="output-block-body flex flex-wrap gap-x-6 gap-y-1">
                <span v-for="(val, i) in (response.data as SqlExecution).bindings" :key="i" class="binding-item">
                  ?{{ i + 1 }} = <code>{{ val }}</code>
                </span>
              </div>
            </div>

            <template v-for="(r, i) in (response.data as SqlExecution).results" :key="i">
              <div class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3" :class="{ 'result-failed': !r.success }">
                <!-- Card header -->
                <div class="result-card-header flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-2 px-3" :class="{ 'header-failed': !r.success }">
                  <template v-if="r.success">
                    <el-icon class="text-[var(--ep-color-success)]"><CircleCheckFilled /></el-icon>
                  </template>
                  <template v-else>
                    <el-icon class="text-[var(--ep-color-danger)]"><CircleCloseFilled /></el-icon>
                  </template>
                  <span>{{ t(r.type === 'SELECT' ? 'mcpService.toolTest.queryResult' : 'mcpService.toolTest.writeResult', { n: i + 1 }) }}</span>
                  <span v-if="r.success && r.type === 'SELECT'" class="ml-auto font-normal">
                    {{ t("mcpService.toolTest.totalRows", { total: (r as SelectResult).total_rows }) }}
                  </span>
                  <span v-if="r.success && r.type === 'WRITE'" class="ml-auto font-normal">
                    {{ t("mcpService.toolTest.affectedRows", { count: (r as WriteResult).affected_rows }) }}
                  </span>
                </div>

                <!-- Card body -->
                <div class="result-card-body p-0">
                  <!-- SELECT success -->
                  <template v-if="r.success && r.type === 'SELECT'">
                    <el-table :data="(r as SelectResult).rows" border size="small" max-height="360" stripe>
                      <el-table-column v-for="(col, ci) in (r as SelectResult).columns" :key="ci"
                        :prop="String(ci)" :label="col" />
                    </el-table>
                  </template>
                  <!-- WRITE success -->
                  <template v-else-if="r.success && r.type === 'WRITE'">
                    <div class="write-result flex items-center gap-2.5 py-3.5 px-4 text-[13px]">
                      {{ t("mcpService.toolTest.affectedRows", { count: (r as WriteResult).affected_rows }) }}
                    </div>
                  </template>
                  <!-- Failed -->
                  <template v-else>
                    <div class="result-error py-3 px-4 text-[13px] text-[var(--ep-color-danger)] font-mono bg-[var(--ep-color-danger-light-9)]">{{ (r as any).error_message }}</div>
                  </template>
                </div>
              </div>
            </template>

            <div v-if="(response.data as SqlExecution).results.length === 0"
              class="text-sm text-[var(--ep-text-color-placeholder)] text-center py-4">
              {{ t("mcpService.toolTest.emptyResult") }}
            </div>
          </template>

          <!-- TABLE_METADATA -->
          <template v-else-if="response.data?.type === 'TABLE_METADATA'">
            <template v-for="entry in (response.data as TableMetadata).tables" :key="entry.table">
              <div class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3" :class="{ 'result-failed': !entry.success }">
                <div class="result-card-header flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-2 px-3" :class="{ 'header-failed': !entry.success }">
                  <template v-if="entry.success">
                    <el-icon class="text-[var(--ep-color-success)]"><CircleCheckFilled /></el-icon>
                  </template>
                  <template v-else>
                    <el-icon class="text-[var(--ep-color-danger)]"><CircleCloseFilled /></el-icon>
                  </template>
                  <span>{{ entry.schema ? entry.schema + '.' : '' }}{{ entry.table }}</span>
                </div>
                <div class="result-card-body p-0">
                  <template v-if="entry.success && entry.columns">
                    <el-table :data="entry.columns" border size="small">
                      <el-table-column prop="name" label="Column" />
                      <el-table-column prop="type" label="Type" width="140" />
                      <el-table-column prop="comment" label="Comment" />
                    </el-table>
                  </template>
                  <template v-else>
                    <div class="result-error py-3 px-4 text-[13px] text-[var(--ep-color-danger)] font-mono bg-[var(--ep-color-danger-light-9)]">{{ entry.error_message }}</div>
                  </template>
                </div>
              </div>
            </template>
          </template>

          <!-- SEARCH_HIT -->
          <template v-else-if="response.data?.type === 'SEARCH_HIT'">
            <el-empty :description="t('mcpService.toolTest.comingSoon')" />
          </template>
        </template>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.section-title::before {
  content: "";
  display: inline-block;
  width: 3px; height: 13px;
  background-color: var(--ep-color-primary);
  border-radius: 2px;
  margin-right: 8px;
  vertical-align: middle;
  margin-top: -1px;
}

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

.error-card {
  border: 1px solid var(--ep-color-danger-light-7);
  border-left: 4px solid var(--ep-color-danger);
  border-radius: 0 8px 8px 0;
  padding: 16px;
  background: var(--ep-color-danger-light-9);
}
.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--ep-color-danger);
  margin-bottom: 10px;
}
.error-category {
  font-family: monospace;
  font-size: 12px;
}
.error-message {
  font-family: monospace;
  font-size: 12px;
  color: var(--ep-color-danger);
  background: var(--ep-bg-color);
  padding: 8px 12px;
  border-radius: 4px;
  margin: 0 0 10px;
  white-space: pre-wrap;
  word-break: break-word;
}
.error-advice {
  font-size: 13px;
  color: var(--ep-text-color-secondary);
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
