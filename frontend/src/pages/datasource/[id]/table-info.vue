<route lang="yaml">
meta:
  activeMenu: /datasource
</route>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus, Search, Setting } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import { listTableInfos, type TableInfoVO } from "~/api/tableinfo.ts";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const route = useRoute("/datasource/[id]/table-info");

// 数据源ID
const datasourceId = ref(route.params.id);
const searchKeyword = ref("");

// 表格数据
const loading = ref(false);
const tableList = ref<TableInfoVO[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 弹窗状态
const metadataDialogVisible = ref(false);
const currentTable = ref<TableInfoVO | null>(null);

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
    ElMessage.error(t("datasource.table.loadFailed"));
  } finally {
    loading.value = false;
  }
};

// 搜索处理
const handleSearch = () => {
  page.value = 1; // 搜索时回到第一页
  loadTables();
};

// 清空搜索
const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadTables();
};

const handleCreate = () => {};

// 配置元数据
const handleConfigMetadata = (table: TableInfoVO) => {
  currentTable.value = { ...table };
  metadataDialogVisible.value = true;
};

// 保存元数据配置
const handleSaveMetadata = async () => {
  try {
    // TODO: 调用后端接口保存
    // await saveTableMetadata(datasourceId.value, currentTable.value);
    ElMessage.success(t("datasource.table.saveSuccess"));
    metadataDialogVisible.value = false;
    await loadTables();
  } catch (error) {
    console.error("保存元数据失败:", error);
    ElMessage.error(t("datasource.table.saveFailed"));
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
      <el-breadcrumb-item :to="{ path: '/datasource' }">
        {{ $t("side.dataSources") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{
        $t("datasource.tableInfo.title")
      }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="flex mb-6">
      <!-- 搜索框 -->
      <div class="flex-1">
        <el-input
          v-model="searchKeyword"
          :placeholder="$t('datasource.page.searchPlaceholder')"
          clearable
          class="max-w-sm"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
        </el-input>
        <el-button :icon="Search" @click="handleSearch">
          {{ $t("common.search") }}
        </el-button>
      </div>

      <!-- 创建按钮 -->
      <div class="flex justify-end">
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          {{ $t("datasource.tableInfo.addTable") }}
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="tableList" v-loading="loading" stripe>
      <el-table-column
        prop="name"
        :label="$t('datasource.tableInfo.tableName')"
        min-width="150"
      />
      <el-table-column
        prop="display_name"
        :label="$t('datasource.tableInfo.displayName')"
        min-width="150"
      />
      <el-table-column
        prop="description"
        :label="$t('datasource.tableInfo.comment')"
        min-width="200"
      />
      <el-table-column
        prop="schema"
        :label="$t('datasource.tableInfo.schema')"
        min-width="120"
      />
      <el-table-column
        prop="updated_at"
        :label="$t('common.updatedAt')"
        min-width="120"
      >
        <template #default="{ row }">
          {{ formatDateTime(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.actions')" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :icon="Setting"
            type="primary"
            @click="handleConfigMetadata(row)"
          >
            {{ $t("datasource.tableInfo.configButton") }}
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

    <!-- 元数据配置弹窗 -->
    <el-dialog
      v-model="metadataDialogVisible"
      :title="$t('datasource.tableInfo.configTitle')"
      width="600px"
    >
      <el-form v-if="currentTable" :model="currentTable" label-width="120px">
        <el-form-item :label="$t('datasource.tableInfo.tableName')">
          <el-input v-model="currentTable.name" disabled />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.displayName')">
          <el-input
            v-model="currentTable.display_name"
            :placeholder="$t('datasource.tableInfo.displayNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.description')">
          <el-input
            v-model="currentTable.description"
            type="textarea"
            :rows="3"
            :placeholder="$t('datasource.tableInfo.descriptionPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataDialogVisible = false">
          {{ $t("common.cancel") }}
        </el-button>
        <el-button type="primary" @click="handleSaveMetadata">
          {{ $t("common.save") }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>