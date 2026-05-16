<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import type { McpServiceVO } from "~/api/mcp-service";
import { listMcpServices } from "~/api/mcp-service";
import { Plus, Search } from "@element-plus/icons-vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const serviceList = ref<McpServiceVO[]>([]);
const dialogVisible = ref(false);
const currentService = ref<McpServiceVO | null>(null);
const searchKeyword = ref("");
const statusFilter = ref("");

const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const statusOptions = [
  { label: t("mcpService.status.draft"), value: "DRAFT" },
  { label: t("mcpService.status.published"), value: "PUBLISHED" },
  { label: t("mcpService.status.disabled"), value: "DISABLED" },
];

const loadServices = async () => {
  try {
    loading.value = true;
    const response = await listMcpServices(
      page.value,
      pageSize.value,
      searchKeyword.value || undefined,
      statusFilter.value || undefined,
    );
    serviceList.value = response.data || [];
    total.value = response.total ?? 0;
  } catch (error) {
    console.error("加载 MCP 服务列表失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadServices();
};

const handleClearSearch = () => {
  searchKeyword.value = "";
  statusFilter.value = "";
  page.value = 1;
  loadServices();
};

const handlePageChange = (p: number) => {
  page.value = p;
  loadServices();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1;
  loadServices();
};

const handleCreate = () => {
  currentService.value = null;
  dialogVisible.value = true;
};

const handleEdit = (service: McpServiceVO) => {
  currentService.value = { ...service };
  dialogVisible.value = true;
};

const handleDetail = (service: McpServiceVO) => {
  router.push({
    path: `/mcp-services/${service.id}`,
  });
};

const handleDelete = async (service: McpServiceVO) => {
  try {
    await ElMessageBox.confirm(
      t("mcpService.deleteConfirmMessage", { name: service.name }),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      },
    );
    // TODO: implement delete API in US-10
    ElMessage.info(t("mcpService.comingSoon"));
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除失败:", error);
    }
  }
};

const handleDialogSuccess = () => {
  dialogVisible.value = false;
  loadServices();
};

onMounted(() => {
  loadServices();
});
</script>

<template>
  <div class="p-5 md:p-6">
    <!-- 头部操作区 -->
    <div class="flex flex-wrap items-center justify-between gap-3 mb-6">
      <div class="flex items-center gap-3">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('mcpService.searchPlaceholder')"
          clearable
          class="!w-96"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
        <el-select
          v-model="statusFilter"
          :placeholder="t('mcpService.status.label')"
          clearable
          class="!w-28"
          @change="handleSearch"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        {{ t("mcpService.createButton") }}
      </el-button>
    </div>

    <McpServiceTable
      :data="serviceList"
      :loading="loading"
      @detail="handleDetail"
      @edit="handleEdit"
      @delete="handleDelete"
    />

    <div class="flex items-center justify-between mt-4">
      <span class="text-gray-500 text-sm">
        {{ t("common.total", { total }) }}
      </span>
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

    <McpServiceDialog
      v-model="dialogVisible"
      :service="currentService"
      @success="handleDialogSuccess"
    />
  </div>
</template>
