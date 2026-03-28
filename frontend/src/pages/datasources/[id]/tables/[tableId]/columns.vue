<route lang="yaml">
meta:
activeMenu: /datasources
</route>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import { listTableColumns, saveColumnMetadata, type TableColumnVO } from "~/api/column";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
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

    columnList.value = resp.data || [];
    total.value = resp.total ?? 0;
  } catch (error) {
    console.error("加载列信息失败:", error);
    ElMessage.error(t("common.loadFailed"));
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

const handleConfigMetadata = (col: TableColumnVO) => {
  currentColumn.value = { ...col };
  metadataDialogVisible.value = true;
};

const handleSaveMetadata = async () => {
  try {
    await saveColumnMetadata(datasourceId.value, tableId.value, currentColumn.value!);
    ElMessage.success(t("common.saveSuccess"));
    metadataDialogVisible.value = false;
    await loadColumns();
  } catch (error) {
    console.error("保存列元数据失败:", error);
    ElMessage.error(t("common.operationFailed"));
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
        {{ t("layout.side.dataSources") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: `/datasources/${datasourceId}/tables` }">
        {{ t("tableInfo.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>
        {{ t("tableInfo.columnSettings") }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="flex items-center justify-between gap-4 mb-6">
      <el-input
          v-model="searchKeyword"
          :placeholder="t('column.searchPlaceholder')"
          clearable
          class="max-w-sm"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <!-- 列表 -->
    <el-table :data="columnList" v-loading="loading" stripe>
      <el-table-column prop="name" :label="t('column.columnName')" min-width="180" />
      <el-table-column prop="display_name" :label="t('column.displayName')" min-width="160" />
      <el-table-column prop="description" :label="t('column.description')" min-width="160" />
      <el-table-column prop="column_type" :label="t('column.type')" min-width="120" />

      <el-table-column prop="updated_at" :label="t('common.updatedAt')" min-width="140">
        <template #default="{ row }">
          {{ row.updated_at ? formatDateTime(row.updated_at) : "-" }}
        </template>
      </el-table-column>

      <el-table-column :label="t('common.actions')" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
              link
              type="primary"
              @click="handleConfigMetadata(row)"
          >
            {{ t('common.edit') }}
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

    <!-- 列元数据配置弹窗 -->
    <el-dialog v-model="metadataDialogVisible" :title="t('column.editTitle')" width="600px">
      <el-form v-if="currentColumn" :model="currentColumn" label-width="120px">
        <el-form-item :label="t('column.columnName')">
          <el-input v-model="currentColumn.name" disabled />
        </el-form-item>

        <el-form-item :label="t('column.displayName')">
          <el-input v-model="currentColumn.display_name" :placeholder="t('column.enterDescription')" />
        </el-form-item>

        <el-form-item :label="t('column.description')">
          <el-input v-model="currentColumn.description" :placeholder="t('column.enterDescription')" />
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