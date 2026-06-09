<route lang="yaml">
meta:
  activeMenu: /subjects
</route>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import type { SubjectVO, UpdateSubjectRequest } from "~/api/subject";
import { getSubject, updateSubject } from "~/api/subject";

const { t } = useI18n();

const route = useRoute("/subjects/[id]");

const subjectId = computed(() => route.params.id as string);

const loading = ref(false);
const saving = ref(false);
const subject = ref<SubjectVO | null>(null);

const formData = ref<UpdateSubjectRequest>({});
const newAlias = ref('')
const newAliasInputVisible = ref(false)
const newAliasInputRef = ref()

const isDirty = computed(() => {
  if (!subject.value) return false
  const s = subject.value
  return (
    formData.value.name !== s.name ||
    (formData.value.description ?? '') !== (s.description ?? '') ||
    JSON.stringify(formData.value.aliases ?? []) !== JSON.stringify(s.aliases ?? [])
  )
})

const loadSubject = async () => {
  try {
    loading.value = true;
    const s = await getSubject(subjectId.value);
    subject.value = s;
    formData.value = {
      name: s.name,
      description: s.description || undefined,
      aliases: s.aliases ? [...s.aliases] : [],
    };
  } catch (error) {
    console.error("Failed to load subject:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const showNewAliasInput = () => {
  newAliasInputVisible.value = true
  nextTick(() => {
    newAliasInputRef.value?.focus()
  })
}

const handleNewAliasConfirm = () => {
  const value = newAlias.value.trim()
  if (value && !formData.value.aliases?.includes(value)) {
    if (!formData.value.aliases) {
      formData.value.aliases = []
    }
    formData.value.aliases.push(value)
  }
  newAlias.value = ''
  newAliasInputVisible.value = false
}

const removeAlias = (alias: string) => {
  formData.value.aliases = formData.value.aliases?.filter(a => a !== alias) || []
}

const handleSave = async () => {
  try {
    saving.value = true;
    await updateSubject(subjectId.value, formData.value);
    ElMessage.success(t("subject.updateSuccess"));
    await loadSubject();
  } catch (error) {
    console.error("Failed to update subject:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  loadSubject();
});
</script>

<template>
  <div class="space-y-4 p-5 md:p-6">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/subjects' }">
        {{ t("subject.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ subject?.name || "" }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card shadow="never" class="border border-[var(--ep-border-color-lighter)]">
      <el-tabs>
      <el-tab-pane :label="t('subject.basicInfo')">
        <div class="max-w-3xl">
          <el-skeleton v-if="loading" :rows="5" animated />
          <template v-else-if="subject">
            <el-form label-position="top">
              <el-form-item :label="'ID'">
                <span class="font-mono text-sm text-[var(--ep-text-color-secondary)]">{{ subject.id }}</span>
              </el-form-item>

              <el-form-item :label="t('common.name')">
                <el-input v-model="formData.name" />
              </el-form-item>

              <el-form-item :label="t('common.aliases')">
                <div class="flex gap-2 flex-wrap">
                  <el-tag
                    v-for="alias in formData.aliases"
                    :key="alias"
                    closable
                    :disable-transitions="false"
                    @close="removeAlias(alias)"
                  >
                    {{ alias }}
                  </el-tag>
                  <el-input
                    v-if="newAliasInputVisible"
                    ref="newAliasInputRef"
                    v-model="newAlias"
                    class="w-20"
                    size="small"
                    @keyup.enter="handleNewAliasConfirm"
                    @blur="handleNewAliasConfirm"
                  />
                  <el-button v-else size="small" @click="showNewAliasInput">
                    + {{ t('common.aliases') }}
                  </el-button>
                </div>
              </el-form-item>

              <el-form-item :label="t('common.description')">
                <el-input
                  v-model="formData.description"
                  type="textarea"
                  :rows="3"
                  :placeholder="t('common.placeholder.description')"
                />
              </el-form-item>

              <el-form-item :label="t('subject.datasource')">
                <span>{{ subject.datasource_name || subject.datasource_id }}</span>
              </el-form-item>
            </el-form>

            <div class="mt-4">
              <el-button type="primary" :loading="saving" :disabled="!isDirty" @click="handleSave">
                {{ t("common.save") }}
              </el-button>
            </div>
          </template>
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
    </el-card>
  </div>
</template>
