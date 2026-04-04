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
import { Plus, Search } from "@element-plus/icons-vue";

const { t } = useI18n();
const router = useRouter();

const loading = ref(false);
const subjectList = ref<SubjectVO[]>([]);
const dialogVisible = ref(false);
const currentSubject = ref<SubjectVO | null>(null);
const searchKeyword = ref("");

const page = ref(1);
const pageSize = ref(12);
const total = ref(0);

const loadSubjects = async () => {
  try {
    loading.value = true;
    const response = await listSubjects(page.value, pageSize.value, undefined);
    let filtered = response.data || [];
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase();
      filtered = filtered.filter((s) => s.name.toLowerCase().includes(kw));
    }
    subjectList.value = filtered;
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
      t("common.deleteConfirmMessage", { name: subject.name }),
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

const handleCardClick = (subject: SubjectVO) => {
  router.push({
    path: `/subjects/${subject.id}`,
  });
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
  <div class="p-5 md:p-6">
    <div class="flex items-center justify-between gap-4 mb-6">
      <el-input
        v-model="searchKeyword"
        :placeholder="t('subject.searchPlaceholder')"
        clearable
        class="max-w-sm"
        @keyup.enter="handleSearch"
        @clear="handleClearSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>

      <el-button type="primary" :icon="Plus" @click="handleCreate">
        {{ t("subject.createButton") }}
      </el-button>
    </div>

    <div v-if="subjectList.length === 0 && !loading" class="text-center text-gray-400 py-12">
      {{ t("subject.noSubject") }}
    </div>

    <div v-else class="subject-grid">
      <SubjectCard
        v-for="subject in subjectList"
        :key="subject.id"
        :subject="subject"
        @click="handleCardClick"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>

    <div v-if="total > 0" class="flex items-center justify-between mt-6">
      <span class="text-gray-500 text-sm">{{ t("common.total", { total }) }}</span>
      <el-pagination
        layout="sizes, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[12, 24, 48, 96]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>

    <SubjectDialog
      v-model="dialogVisible"
      :subject="currentSubject"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<style scoped>
.subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
</style>
