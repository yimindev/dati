<route lang="yaml">
meta:
  activeMenu: /datasources
</route>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import { listTableInfos, getAddedTableNames, batchAddTables, syncColumns, deleteTable, type TableInfoVO } from "~/api/tableinfo.ts";
import { getSchemas, getTables } from "~/api/datasource.ts";
import { formatDateTime } from "~/composables";

const { t } = useI18n();

const router = useRouter();
const route = useRoute("/datasources/[id]/tables/");

const datasourceId = ref(route.params.id);
const searchKeyword = ref("");

const loading = ref(false);
const tableList = ref<TableInfoVO[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const metadataDialogVisible = ref(false);
const currentTable = ref<TableInfoVO | null>(null);

const addTableDialogVisible = ref(false);
const schemas = ref<string[]>([]);
const selectedSchema = ref("");
const availableTables = ref<string[]>([]);
const selectedTables = ref<string[]>([]);
const addedTableNames = ref<string[]>([]);
const schemaLoading = ref(false);
const tablesLoading = ref(false);
const addTableLoading = ref(false);

// 加载表列表（待实现接口）
const loadTables = async () => {
  try {
    loading.value = true;
    const response = await listTableInfos(
      datasourceId.value,
      page.value,
      pageSize.value,
      searchKeyword.value,
    );
    tableList.value = response.data || [];
    total.value = response.total ?? 0;
  } catch (error) {
    console.error("加载表列表失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadTables();
};

const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadTables();
};

const handleOpenAddTableDialog = async () => {
  addTableDialogVisible.value = true;
  selectedSchema.value = "";
  availableTables.value = [];
  selectedTables.value = [];
  
  try {
    schemaLoading.value = true;
    schemas.value = await getSchemas(datasourceId.value);
    addedTableNames.value = await getAddedTableNames(datasourceId.value);
  } catch (error) {
    console.error("加载 schema 列表失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    schemaLoading.value = false;
  }
};

const handleSchemaChange = async (schema: string) => {
  if (!schema) {
    availableTables.value = [];
    return;
  }
  
  try {
    tablesLoading.value = true;
    availableTables.value = await getTables(datasourceId.value, schema);
    selectedTables.value = [];
  } catch (error) {
    console.error("加载表列表失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    tablesLoading.value = false;
  }
};

const handleSelectAll = () => {
  const unaddedTables = availableTables.value.filter(t => !addedTableNames.value.includes(t));
  selectedTables.value = [...unaddedTables];
};

const handleDeselectAll = () => {
  selectedTables.value = [];
};

const handleBatchAdd = async () => {
  if (selectedTables.value.length === 0) {
    ElMessage.warning(t("tableInfo.selectAtLeastOne"));
    return;
  }
  
  try {
    addTableLoading.value = true;
    const tables = selectedTables.value.map(name => ({ name, schema: selectedSchema.value }));
    await batchAddTables(datasourceId.value, tables);
    ElMessage.success(t("tableInfo.addSuccess"));
    addTableDialogVisible.value = false;
    await loadTables();
  } catch (error) {
    console.error("批量添加表失败:", error);
    ElMessage.error(t("tableInfo.addFailed"));
  } finally {
    addTableLoading.value = false;
  }
};

const handleSyncColumns = async (table: TableInfoVO) => {
  try {
    await syncColumns(datasourceId.value, table.id);
    ElMessage.success(t("tableInfo.syncSuccess"));
  } catch (error) {
    console.error("同步列信息失败:", error);
    ElMessage.error(t("tableInfo.syncFailed"));
  }
};

const handleRemoveTable = async (table: TableInfoVO) => {
  try {
    await ElMessageBox.confirm(
      t("tableInfo.removeConfirm", { name: table.name }),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      }
    );
    await deleteTable(datasourceId.value, table.id);
    ElMessage.success(t("tableInfo.removeSuccess"));
    await loadTables();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除表失败:", error);
      ElMessage.error(t("tableInfo.removeFailed"));
    }
  }
};

// 配置元数据
const handleConfigMetadata = (table: TableInfoVO) => {
  currentTable.value = { ...table };
  metadataDialogVisible.value = true;
};

const handleColumnManage = (table: TableInfoVO) => {
  router.push({
    path: `/datasources/${table.datasource_id}/tables/${table.id}/columns`,
  });
};

// 保存元数据配置
const handleSaveMetadata = async () => {
  try {
    // TODO: 调用后端接口保存
    // await saveTableMetadata(datasourceId.value, currentTable.value);
    ElMessage.success(t("common.saveSuccess"));
    metadataDialogVisible.value = false;
    await loadTables();
  } catch (error) {
    console.error("保存元数据失败:", error);
    ElMessage.error(t("common.operationFailed"));
  }
};

// 分页事件
const handlePageChange = (p: number) => {
  page.value = p;
  loadTables();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1;
  loadTables();
};

onMounted(() => {
  loadTables();
});
</script>

<template>
  <div class="p-5 md:p-6">
    <el-breadcrumb separator="/" class="mb-6">
      <el-breadcrumb-item :to="{ path: '/datasources' }">
        {{ t("layout.side.dataSources") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{
        t("tableInfo.title")
      }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="flex items-center justify-between gap-4 mb-6">
      <!-- 搜索框 -->
      <el-input
        v-model="searchKeyword"
        :placeholder="t('datasource.searchPlaceholder')"
        clearable
        class="max-w-sm"
        @keyup.enter="handleSearch"
        @clear="handleClearSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <!-- 创建按钮 -->
      <el-button type="primary" :icon="Plus" @click="handleOpenAddTableDialog">
        {{ t("tableInfo.addTable") }}
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableList" v-loading="loading" stripe>
      <el-table-column
        prop="name"
        :label="t('common.tableName')"
        min-width="150"
      />
      <el-table-column
        prop="display_name"
        :label="t('tableInfo.displayName')"
        min-width="150"
      />
      <el-table-column
        prop="description"
        :label="t('common.description')"
        min-width="150"
      />
      <el-table-column
        prop="schema"
        :label="t('common.schema')"
        min-width="120"
      />
      <el-table-column
        prop="updated_at"
        :label="t('common.updatedAt')"
        min-width="120"
      >
        <template #default="{ row }">
          {{ formatDateTime(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('common.actions')"
        min-width="150"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button type="primary" link @click="handleColumnManage(row)">
            {{ t("tableInfo.columnSettings") }}
          </el-button>

          <el-button link type="primary" @click="handleConfigMetadata(row)">
            {{ t("common.edit") }}
          </el-button>

          <el-button link type="primary" @click="handleSyncColumns(row)">
            {{ t("tableInfo.syncColumns") }}
          </el-button>

          <el-button link type="danger" @click="handleRemoveTable(row)">
            {{ t("common.remove") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="flex items-center justify-between mt-4">
      <span class="text-gray-500 text-sm">{{ t('common.total', { total }) }}</span>
      <el-pagination
        layout="sizes, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>

    <!-- 元数据配置弹窗 -->
    <el-dialog
      v-model="metadataDialogVisible"
      :title="t('tableInfo.configInfo')"
      width="600px"
    >
      <el-form v-if="currentTable" :model="currentTable" label-width="120px">
        <el-form-item :label="t('common.tableName')">
          <el-input v-model="currentTable.name" disabled />
        </el-form-item>
        <el-form-item :label="t('tableInfo.displayName')">
          <el-input
            v-model="currentTable.display_name"
            :placeholder="t('common.placeholder.name')"
          />
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input
            v-model="currentTable.description"
            :placeholder="t('common.placeholder.description')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataDialogVisible = false">
          {{ t("common.cancel") }}
        </el-button>
        <el-button type="primary" @click="handleSaveMetadata">
          {{ t("common.save") }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 添加表弹窗 -->
    <el-dialog
      v-model="addTableDialogVisible"
      :title="t('tableInfo.addTable')"
      width="600px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item :label="t('common.schema')" required>
          <el-select
            v-model="selectedSchema"
            :placeholder="t('tableInfo.selectSchema')"
            :loading="schemaLoading"
            class="w-full"
            @change="handleSchemaChange"
          >
            <el-option
              v-for="schema in schemas"
              :key="schema"
              :label="schema"
              :value="schema"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('tableInfo.availableTables')" v-if="selectedSchema">
          <div v-loading="tablesLoading" class="w-full min-h-[200px]">
            <div class="mb-2 flex gap-2">
              <el-button size="small" @click="handleSelectAll">
                {{ t('common.all') }}
              </el-button>
              <el-button size="small" @click="handleDeselectAll">
                {{ t('common.none') }}
              </el-button>
            </div>
            <el-checkbox-group v-model="selectedTables" class="w-full">
              <el-checkbox
                v-for="table in availableTables"
                :key="table"
                :label="table"
                :disabled="addedTableNames.includes(table)"
                class="w-full"
              >
                {{ table }}
                <span v-if="addedTableNames.includes(table)" class="text-gray-400 text-xs ml-2">
                  ({{ t('tableInfo.alreadyAdded') }})
                </span>
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>

        <el-form-item v-if="selectedSchema">
          <div class="text-sm text-gray-600">
            {{ t('tableInfo.selectedCount', { count: selectedTables.length }) }}
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addTableDialogVisible = false">
          {{ t("common.cancel") }}
        </el-button>
        <el-button 
          type="primary" 
          :loading="addTableLoading" 
          :disabled="selectedTables.length === 0"
          @click="handleBatchAdd"
        >
          {{ t("tableInfo.addSelected") }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>