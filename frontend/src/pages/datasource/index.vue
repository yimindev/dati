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
import { Plus, Search } from "@element-plus/icons-vue";

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
    console.error("加载数据源失败:", error);
    ElMessage.error(t("datasource.messages.loadFailed"));
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
    path: `/datasource/${datasource.id}/table-info`,
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
      ElMessage.success(t("datasource.messages.testSuccess"));
    } else {
      ElMessage.error(t("datasource.messages.testFailed"));
    }
  } catch (error) {
    console.error("测试连接失败:", error);
    ElMessage.error(t("datasource.messages.testFailed"));
  } finally {
    loading.value = false;
  }
};

// 删除数据源
const handleDelete = async (datasource: DatasourceVO) => {
  try {
    await ElMessageBox.confirm(
      t("datasource.page.deleteConfirmMessage", { name: datasource.name }),
      t("datasource.page.deleteConfirmTitle"),
      {
        confirmButtonText: t("datasource.common.confirm"),
        cancelButtonText: t("datasource.common.cancel"),
        type: "warning",
      },
    );

    await deleteDataSource(datasource.id);
    ElMessage.success(t("datasource.messages.deleteSuccess"));
    await loadDatasources();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除失败:", error);
      ElMessage.error(t("datasource.messages.deleteFailed"));
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
  <div class="p-5 md:p-6">
    <!-- 头部操作区 -->
    <div class="flex gap-4 mb-6">
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
          {{ $t("datasource.page.createButton") }}
        </el-button>
      </div>
    </div>

    <!-- 数据表格（只做展示与事件触发） -->
    <DatasourceTable
      :data="datasourceList"
      :loading="loading"
      @edit="handleEdit"
      @delete="handleDelete"
      @test-connection="handleTestConnection"
      @table-manage="handleTableManage"
    />

    <!-- 分页（父级集中管理） -->
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

    <!-- 创建/编辑弹窗（父级集中管理） -->
    <DatasourceDialog
      v-model="dialogVisible"
      :datasource="currentDatasource"
      @success="handleDialogSuccess"
    />
  </div>
</template>
