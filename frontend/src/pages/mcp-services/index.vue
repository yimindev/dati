<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import type { McpServiceVO } from "~/api/mcp-service";
import { deleteMcpService, listMcpServices } from "~/api/mcp-service";
import { Plus, Refresh, Search } from "@element-plus/icons-vue";
import AuthDialog from "~/components/common/AuthDialog.vue";
import DataTableShell from "~/components/common/DataTableShell.vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const serviceList = ref<McpServiceVO[]>([]);
const dialogVisible = ref(false);
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

const activeFilters = computed(
  () => Number(Boolean(searchKeyword.value)) + Number(Boolean(statusFilter.value)),
);

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
    console.error("Failed to load MCP services:", error);
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

const handleRefresh = () => {
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
  dialogVisible.value = true;
};

const handleDetail = (service: McpServiceVO) => {
  router.push({
    path: `/mcp-services/${service.id}`,
  });
};

const authDialogVisible = ref(false);
const authResourceId = ref("");

const handleAuthorize = (service: McpServiceVO) => {
  authResourceId.value = service.id;
  authDialogVisible.value = true;
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
    await deleteMcpService(service.id);
    ElMessage.success(t("mcpService.deleteSuccess"));
    // 删除的是当前页最后一条时回退一页，避免停留在空页
    if (serviceList.value.length === 1 && page.value > 1) {
      page.value -= 1;
    }
    loadServices();
  } catch (error: any) {
    if (error !== "cancel" && error !== "close") {
      console.error("Failed to delete service:", error);
      ElMessage.error(error?.message || t("common.operationFailed"));
    }
  }
};

const handleDialogSuccess = (id: string) => {
  dialogVisible.value = false;
  router.push({ path: `/mcp-services/${id}` });
};

onMounted(() => {
  loadServices();
});
</script>

<template>
  <div class="list-page">
    <div class="page-heading">
      <div>
        <h1>{{ t("mcpService.title") }}</h1>
        <p>{{ t("mcpService.listSubtitle") }}</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">
          {{ t("common.refresh") }}
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          {{ t("mcpService.createButton") }}
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-fields">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('mcpService.searchPlaceholder')"
          clearable
          class="toolbar-search"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="statusFilter"
          :placeholder="t('mcpService.status.all')"
          clearable
          class="toolbar-status"
          @change="handleSearch"
          @clear="handleSearch"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button type="primary" plain :icon="Search" @click="handleSearch">
          {{ t("common.search") }}
        </el-button>
        <el-button v-if="activeFilters > 0" link @click="handleClearSearch">
          {{ t("common.clear") }}
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
      <McpServiceTable
        :data="serviceList"
        :has-filter="activeFilters > 0"
        @detail="handleDetail"
        @delete="handleDelete"
        @authorize="handleAuthorize"
      />
    </DataTableShell>

    <AuthDialog
      v-model:visible="authDialogVisible"
      resource-type="MCP_SERVICE"
      :resource-id="authResourceId"
    />

    <McpServiceDialog v-model="dialogVisible" @success="handleDialogSuccess" />
  </div>
</template>

<style scoped>
.toolbar-status {
  width: 160px;
}

@media (max-width: 768px) {
  .toolbar-status {
    width: 100%;
  }
}
</style>
