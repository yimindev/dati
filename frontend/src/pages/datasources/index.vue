<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import type { DatasourceVO } from "~/api/datasource";
import {
  deleteDataSource,
  listDataSources,
  testConnection,
} from "~/api/datasource";
import { Plus, Search, Refresh } from "@element-plus/icons-vue";
import AuthDialog from "~/components/common/AuthDialog.vue";

const { t } = useI18n();
const router = useRouter();

// 响应式数据
const loading = ref(false);
const datasourceList = ref<DatasourceVO[]>([]);
const dialogVisible = ref(false);
const currentDatasource = ref<DatasourceVO | null>(null);
const searchKeyword = ref("");

// 分页状态（由父组件统一管理）
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 授权弹窗状态
const authDialogVisible = ref(false);
const authResourceId = ref("");

const handleAuthorize = (datasource: DatasourceVO) => {
  authResourceId.value = datasource.id;
  authDialogVisible.value = true;
};

// 加载数据源列表（携带分页参数）
const loadDatasources = async () => {
  try {
    loading.value = true;
    const response = await listDataSources(
      page.value,
      pageSize.value,
      searchKeyword.value,
    );
    // 假设后端返回 { data: DatasourceVO[], total: number }
    datasourceList.value = response.data || [];
    total.value = response.total ?? 0;
  } catch (error) {
    console.error("Failed to load data source:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

// 搜索处理
const handleSearch = () => {
  page.value = 1; // 搜索时回到第一页
  loadDatasources();
};

// 清空搜索
const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadDatasources();
};

// 分页事件
const handlePageChange = (p: number) => {
  page.value = p;
  loadDatasources();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1; // 切换每页数量时通常回到第1页
  loadDatasources();
};

// 刷新
const handleRefresh = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadDatasources();
};

// 创建数据源
const handleCreate = () => {
  currentDatasource.value = null;
  dialogVisible.value = true;
};

// 编辑数据源
const handleEdit = (datasource: DatasourceVO) => {
  currentDatasource.value = { ...datasource };
  dialogVisible.value = true;
};

const handleTableManage = (datasource: DatasourceVO) => {
  router.push({
    path: `/datasources/${datasource.id}/tables`,
  });
};

// 测试连接
const handleTestConnection = async (datasource: DatasourceVO) => {
  try {
    const payload = {
      jdbc_url: datasource.jdbc_url,
      username: datasource.username,
      type: datasource.type,
      // 密码通常不在表格中保存/回显，如需测试完整连接可引导到“编辑”弹窗内测试
    };

    loading.value = true;
    const result = await testConnection(payload);

    if (result) {
      ElMessage.success(t("datasource.testSuccess"));
    } else {
      ElMessage.error(t("datasource.testFailed"));
    }
  } catch (error) {
    console.error("Failed to test connection:", error);
    ElMessage.error(t("datasource.testFailed"));
  } finally {
    loading.value = false;
  }
};

// 删除数据源
const handleDelete = async (datasource: DatasourceVO) => {
  try {
    await ElMessageBox.confirm(
      t("datasource.deleteConfirmMessage", { name: datasource.name }),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      },
    );

    await deleteDataSource(datasource.id);
    ElMessage.success(t("common.deleteSuccess"));
    await loadDatasources();
  } catch (error) {
    if (error !== "cancel") {
      console.error("Failed to delete:", error);
      ElMessage.error(t("common.operationFailed"));
    }
  }
};

// 弹窗成功回调
const handleDialogSuccess = () => {
  dialogVisible.value = false;
  loadDatasources();
};

// 页面加载时获取数据
onMounted(() => {
  loadDatasources();
});
</script>

<template>
  <div class="list-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('datasource.title') }}</h1>
        <p>{{ t('datasource.subtitle') }}</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">
          {{ t("common.refresh") }}
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          {{ t("datasource.createButton") }}
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-fields">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('datasource.searchPlaceholder')"
          clearable
          class="toolbar-search"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" plain :icon="Search" @click="handleSearch">
          {{ t("common.search") }}
        </el-button>
      </div>
    </div>

    <DataTableShell
      :loading="loading"
      :total="total"
      :page="page"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    >
      <DatasourceTable
        :data="datasourceList"
        @edit="handleEdit"
        @delete="handleDelete"
        @test-connection="handleTestConnection"
        @table-manage="handleTableManage"
        @authorize="handleAuthorize"
      />
    </DataTableShell>

    <DatasourceDialog
      v-model="dialogVisible"
      :datasource="currentDatasource"
      @success="handleDialogSuccess"
    />

    <AuthDialog
      v-model:visible="authDialogVisible"
      resource-type="DATA_SOURCE"
      :resource-id="authResourceId"
    />
  </div>
</template>
