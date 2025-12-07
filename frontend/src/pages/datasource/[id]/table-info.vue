<route lang="yaml">
meta:
  activeMenu: /datasource
</route>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Setting } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const route = useRoute();

// 数据源ID
const datasourceId = ref(route.params.id);
const datasourceName = ref(""); // 数据源名称

// 表格数据
const loading = ref(false);
const tableList = ref<TableMetadata[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 弹窗状态
const metadataDialogVisible = ref(false);
const currentTable = ref<TableMetadata | null>(null);

// 表元数据类型定义
interface TableMetadata {
  id?: string;
  tableName: string;
  tableComment?: string;
  schemaName?: string;
  tableType?: string;
  rowCount?: number;
  createTime?: string;
  updateTime?: string;
  // 元数据配置字段
  displayName?: string;
  description?: string;
  tags?: string[];
  category?: string;
  isVisible?: boolean;
}

// 加载表列表（待实现接口）
const loadTables = async () => {
  try {
    loading.value = true;
    // TODO: 调用后端接口
    // const response = await listTables(datasourceId.value, page.value, pageSize.value);
    // tableList.value = response.data || [];
    // total.value = response.total ?? 0;
    // datasourceName.value = response.datasourceName || "";

    // 模拟数据
    tableList.value = [
      {
        tableName: "user",
        tableComment: "用户表",
        schemaName: "public",
        tableType: "TABLE",
        rowCount: 1500,
        displayName: "用户信息表",
        isVisible: true,
      },
      {
        tableName: "order",
        tableComment: "订单表",
        schemaName: "public",
        tableType: "TABLE",
        rowCount: 5000,
        isVisible: true,
      },
    ];
    total.value = 2;
    datasourceName.value = "示例数据源";
  } catch (error) {
    console.error("加载表列表失败:", error);
    ElMessage.error(t('datasource.table.loadFailed'));
  } finally {
    loading.value = false;
  }
};

// 刷新表列表
const handleRefresh = () => {
  // loadDatasources();
};

// 配置元数据
const handleConfigMetadata = (table: TableMetadata) => {
  currentTable.value = { ...table };
  metadataDialogVisible.value = true;
};

// 保存元数据配置
const handleSaveMetadata = async () => {
  try {
    // TODO: 调用后端接口保存
    // await saveTableMetadata(datasourceId.value, currentTable.value);
    ElMessage.success(t('datasource.table.saveSuccess'));
    metadataDialogVisible.value = false;
    await loadTables();
  } catch (error) {
    console.error("保存元数据失败:", error);
    ElMessage.error(t('datasource.table.saveFailed'));
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
        {{ $t('side.dataSources') }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ $t('datasource.tableInfo.title') }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="mb-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold">{{ datasourceName }} </h2>
          <p class="text-sm text-gray-500 mt-1">
            {{ $t('datasource.tableInfo.subtitle') }}
          </p>
        </div>
        <el-button type="primary" @click="handleRefresh">
          {{ $t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="tableList" v-loading="loading" stripe>
      <el-table-column prop="tableName" :label="$t('datasource.tableInfo.tableName')" min-width="150" />
      <el-table-column prop="displayName" :label="$t('datasource.tableInfo.displayName')" min-width="150" />
      <el-table-column prop="tableComment" :label="$t('datasource.tableInfo.comment')" min-width="200" />
      <el-table-column prop="schemaName" :label="$t('datasource.tableInfo.schema')" width="120" />
      <el-table-column prop="tableType" :label="$t('datasource.tableInfo.type')" width="100" />
      <el-table-column :label="$t('datasource.tableInfo.actions')" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :icon="Setting"
            type="primary"
            @click="handleConfigMetadata(row)"
          >
            {{ $t('datasource.tableInfo.configButton') }}
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
      <el-form
        v-if="currentTable"
        :model="currentTable"
        label-width="120px"
      >
        <el-form-item :label="$t('datasource.tableInfo.tableName')">
          <el-input v-model="currentTable.tableName" disabled />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.displayName')">
          <el-input v-model="currentTable.displayName" :placeholder="$t('datasource.tableInfo.displayNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.description')">
          <el-input
            v-model="currentTable.description"
            type="textarea"
            :rows="3"
            :placeholder="$t('datasource.tableInfo.descriptionPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.category')">
          <el-input v-model="currentTable.category" :placeholder="$t('datasource.tableInfo.categoryPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('datasource.tableInfo.visible')">
          <el-switch v-model="currentTable.isVisible" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataDialogVisible = false">
          {{ $t('common.cancel') }}
        </el-button>
        <el-button type="primary" @click="handleSaveMetadata">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>