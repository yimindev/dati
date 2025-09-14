<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { DatasourceVO } from "~/api/datasource";
import {
  deleteDataSource,
  listDataSources,
  testConnection,
} from "~/api/datasource";
import { Plus } from "@element-plus/icons-vue";

// 响应式数据
const loading = ref(false);
const datasourceList = ref<DatasourceVO[]>([]);
const dialogVisible = ref(false);
const currentDatasource = ref<DatasourceVO | null>(null);

// 分页状态（由父组件统一管理）
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 加载数据源列表（携带分页参数）
const loadDatasources = async () => {
  try {
    loading.value = true;
    const response = await listDataSources(page.value, pageSize.value);
    // 假设后端返回 { data: DatasourceVO[], total: number }
    datasourceList.value = response.data || [];
    total.value = response.total ?? 0;
  } catch (error) {
    console.error("加载数据源失败:", error);
    ElMessage.error("加载数据源失败");
  } finally {
    loading.value = false;
  }
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
      ElMessage.success("连接测试成功");
    } else {
      ElMessage.error("连接测试失败");
    }
  } catch (error) {
    console.error("测试连接失败:", error);
    ElMessage.error("连接测试失败");
  } finally {
    loading.value = false;
  }
};

// 删除数据源
const handleDelete = async (datasource: DatasourceVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据源 "${datasource.name}" 吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    await deleteDataSource(datasource.id);
    ElMessage.success("删除成功");
    await loadDatasources();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除失败:", error);
      ElMessage.error("删除失败");
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
    <div class="flex justify-end mb-6">
      <el-button type="primary" :icon="Plus" @click="handleCreate"> 创建数据源 </el-button>
    </div>

    <!-- 数据表格（只做展示与事件触发） -->
    <DatasourceTable
      :data="datasourceList"
      :loading="loading"
      @edit="handleEdit"
      @delete="handleDelete"
      @test-connection="handleTestConnection"
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
