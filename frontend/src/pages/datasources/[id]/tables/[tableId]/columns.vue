<route lang="yaml">
meta:
activeMenu: /datasources
</route>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Search, Setting } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import { listTableColumns, type TableColumnVO } from "~/api/column";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const router = useRouter();
const route = useRoute("/datasources/[id]/tables/[tableId]/columns");

const datasourceId = ref(route.params.id);
const tableId = computed(() => route.params.tableId);

const searchKeyword = ref("");
const loading = ref(false);

const columnList = ref<TableColumnVO[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 弹窗状态（列元数据配置）
const metadataDialogVisible = ref(false);
const currentColumn = ref<TableColumnVO | null>(null);

const loadColumns = async () => {
  try {
    loading.value = true;
    const resp = await listTableColumns(
        datasourceId.value,
        tableId.value,
        page.value,
        pageSize.value,
        searchKeyword.value,
    );

    // 兼容后端返回：{ data, total } 或直接数组
    const payload = resp?.data;
    const data = Array.isArray(payload) ? payload : (payload?.data ?? []);
    const ttl =
        typeof payload?.total === "number"
            ? payload.total
            : Array.isArray(data)
                ? data.length
                : 0;

    columnList.value = data || [];
    total.value = ttl;
  } catch (error) {
    console.error("加载列信息失败:", error);
    ElMessage.error(t("datasource.table.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadColumns();
};

const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadColumns();
};

const handlePageChange = (p: number) => {
  page.value = p;
  loadColumns();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1;
  loadColumns();
};

const handleBack = () => {
  router.back();
};

const handleConfigMetadata = (col: TableColumnVO) => {
  currentColumn.value = { ...col };
  metadataDialogVisible.value = true;
};

const handleSaveMetadata = async () => {
  try {
    // TODO: 调用后端接口保存列级业务元数据
    // await saveColumnMetadata(datasourceId.value, schema.value, tableName.value, currentColumn.value)
    ElMessage.success(t("datasource.table.saveSuccess"));
    metadataDialogVisible.value = false;
    await loadColumns();
  } catch (error) {
    console.error("保存列元数据失败:", error);
    ElMessage.error(t("datasource.table.saveFailed"));
  }
};

onMounted(() => {
  loadColumns();
});
</script>

<template>
  <div class="p-5 md:p-6">
    <el-breadcrumb separator="/" class="mb-6">
      <el-breadcrumb-item :to="{ path: '/datasources' }">
        {{ t("side.dataSources") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: `/datasources/${datasourceId}/tables` }">
        {{ t("datasource.tableInfo.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>
        {{ t("datasource.tableInfo.columnSettings") }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-6">

      <div class="flex items-center gap-2">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索列名 / 显示名 / 注释"
            clearable
            class="w-72"
            @keyup.enter="handleSearch"
            @clear="handleClearSearch"
        />
        <el-button :icon="Search" @click="handleSearch">
          {{ t("common.search") }}
        </el-button>
      </div>
    </div>

    <!-- 列表 -->
    <el-table :data="columnList" v-loading="loading" stripe>
      <el-table-column prop="name" label="列名" min-width="180" />
      <el-table-column prop="display_name" label="显示名" min-width="160" />
      <el-table-column prop="data_type" label="类型" min-width="120" />
      <el-table-column prop="description" label="注释" min-width="240" />

      <el-table-column prop="updated_at" :label="t('common.updatedAt')" min-width="140">
        <template #default="{ row }">
          {{ row.updated_at ? formatDateTime(row.updated_at) : "-" }}
        </template>
      </el-table-column>

      <el-table-column :label="t('common.actions')" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
              size="small"
              :icon="Setting"
              type="primary"
              @click="handleConfigMetadata(row)"
          >
            配置
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="flex justify-end mt-4">
      <el-pagination
          layout="total, sizes, prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
      />
    </div>

    <!-- 列元数据配置弹窗（示例：只配显示名+注释） -->
    <el-dialog v-model="metadataDialogVisible" title="配置列元数据" width="600px">
      <el-form v-if="currentColumn" :model="currentColumn" label-width="120px">
        <el-form-item label="列名">
          <el-input v-model="currentColumn.name" disabled />
        </el-form-item>

        <el-form-item label="显示名">
          <el-input v-model="currentColumn.display_name" placeholder="请输入列显示名" />
        </el-form-item>

        <el-form-item label="注释">
          <el-input
              v-model="currentColumn.description"
              type="textarea"
              :rows="3"
              placeholder="请输入列注释/说明"
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
  </div>
</template>