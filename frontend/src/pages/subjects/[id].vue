<route lang="yaml">
meta:
  activeMenu: /subjects
</route>

<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import type { SubjectVO, UpdateSubjectRequest } from "~/api/subject";
import { getSubject, updateSubject } from "~/api/subject";

const { t } = useI18n();

const route = useRoute("/subjects/[id]");

const subjectId = computed(() => route.params.id as string);

const loading = ref(false);
const subject = ref<SubjectVO | null>(null);

const isEditing = ref(false);
const editForm = ref<UpdateSubjectRequest>({});
const saveLoading = ref(false);

const loadSubject = async () => {
  try {
    loading.value = true;
    subject.value = await getSubject(subjectId.value);
  } catch (error) {
    console.error("Failed to load subject:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleEdit = () => {
  if (!subject.value) return;
  editForm.value = {
    name: subject.value.name,
    description: subject.value.description || undefined,
  };
  isEditing.value = true;
};

const handleCancel = () => {
  isEditing.value = false;
  editForm.value = {};
};

const handleSave = async () => {
  try {
    saveLoading.value = true;
    await updateSubject(subjectId.value, editForm.value);
    ElMessage.success(t("subject.updateSuccess"));
    isEditing.value = false;
    await loadSubject();
  } catch (error) {
    console.error("Failed to update subject:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    saveLoading.value = false;
  }
};

onMounted(() => {
  loadSubject();
});
</script>

<template>
  <div class="p-5 md:p-6">
    <el-breadcrumb separator="/" class="mb-6">
      <el-breadcrumb-item :to="{ path: '/subjects' }">
        {{ t("subject.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ subject?.name || "" }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-tabs>
      <el-tab-pane :label="t('subject.basicInfo')">
        <div class="basic-info" v-loading="loading">
          <el-form v-if="subject" label-width="120px">
            <el-form-item :label="t('common.name')">
              <template v-if="isEditing">
                <el-input v-model="editForm.name" />
              </template>
              <span v-else>{{ subject.name }}</span>
            </el-form-item>

            <el-form-item :label="t('common.description')">
              <template v-if="isEditing">
                <el-input
                  v-model="editForm.description"
                  type="textarea"
                  :rows="3"
                  :placeholder="t('common.placeholder.description')"
                />
              </template>
              <span v-else>{{ subject.description || "-" }}</span>
            </el-form-item>

            <el-form-item :label="t('subject.datasource')">
              <span>{{ subject.datasource_name || subject.datasource_id }}</span>
            </el-form-item>
          </el-form>

          <div class="mt-4" v-if="!isEditing">
            <el-button type="primary" @click="handleEdit">
              {{ t("common.edit") }}
            </el-button>
          </div>

          <div class="mt-4" v-else>
            <el-button @click="handleCancel">{{ t("common.cancel") }}</el-button>
            <el-button type="primary" :loading="saveLoading" @click="handleSave">
              {{ t("common.save") }}
            </el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('subject.tableManagement')">
        <SubjectTableList
          v-if="subject?.datasource_id"
          :subject-id="subjectId"
          :datasource-id="subject.datasource_id"
        />
      </el-tab-pane>

      <el-tab-pane :label="t('subject.termManagement')">
        <TermManager :subject-id="subjectId" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.basic-info {
  max-width: 600px;
}
</style>
