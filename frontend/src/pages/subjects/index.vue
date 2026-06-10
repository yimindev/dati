<route lang="yaml">
meta:
  activeMenu: /subjects
</route>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import type { SubjectVO } from "~/api/subject";
import { deleteSubject, listSubjects } from "~/api/subject";
import { Plus, Search, Refresh } from "@element-plus/icons-vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const subjectList = ref<SubjectVO[]>([]);
const dialogVisible = ref(false);
const currentSubject = ref<SubjectVO | null>(null);
const searchKeyword = ref("");

const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

const loadSubjects = async () => {
  try {
    loading.value = true;
    const keyword = searchKeyword.value.trim() || undefined;
    const response = await listSubjects(
      page.value,
      pageSize.value,
      keyword,
    );
    subjectList.value = response.data || [];
    total.value = response.total ?? 0;
  } catch (error) {
    console.error("加载主题失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadSubjects();
};

const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadSubjects();
};

const handlePageChange = (p: number) => {
  page.value = p;
  loadSubjects();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1;
  loadSubjects();
};

const handleRefresh = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadSubjects();
};

const handleCreate = () => {
  currentSubject.value = null;
  dialogVisible.value = true;
};

const handleEdit = (subject: SubjectVO) => {
  currentSubject.value = { ...subject };
  dialogVisible.value = true;
};

const handleDelete = async (subject: SubjectVO) => {
  try {
    await ElMessageBox.confirm(
      t("common.confirmDelete", { name: subject.name }),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      },
    );

    await deleteSubject(subject.id);
    ElMessage.success(t("common.deleteSuccess"));
    await loadSubjects();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除失败:", error);
      ElMessage.error(t("common.operationFailed"));
    }
  }
};

const handleDetail = (subject: SubjectVO) => {
  router.push({ path: `/subjects/${subject.id}` });
};

const handleDialogSuccess = () => {
  dialogVisible.value = false;
  loadSubjects();
};

onMounted(() => {
  loadSubjects();
});
</script>

<template>
  <div class="list-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('subject.title') }}</h1>
        <p>{{ t('subject.subtitle') }}</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">
          {{ t("common.refresh") }}
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          {{ t("subject.createButton") }}
        </el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-fields">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('subject.searchPlaceholder')"
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
      <SubjectTable
        :data="subjectList"
        :loading="loading"
        @detail="handleDetail"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </DataTableShell>

    <SubjectDialog
      v-model="dialogVisible"
      :subject="currentSubject"
      @success="handleDialogSuccess"
    />
  </div>
</template>


