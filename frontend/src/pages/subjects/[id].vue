<route lang="yaml">
meta:
  activeMenu: /subjects
</route>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import { DocumentCopy, Connection } from "@element-plus/icons-vue";
import type { SubjectVO, UpdateSubjectRequest } from "~/api/subject";
import { getSubject, updateSubject } from "~/api/subject";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const route = useRoute("/subjects/[id]");

const subjectId = computed(() => route.params.id as string);

const loading = ref(false);
const saving = ref(false);
const subject = ref<SubjectVO | null>(null);

const formData = ref<UpdateSubjectRequest>({});
const newAlias = ref("");
const newAliasInputVisible = ref(false);
const newAliasInputRef = ref();

const activeTab = ref("basic");

const tabs = computed(() => [
  { key: "basic", label: t("subject.basicInfo") },
  { key: "tables", label: t("subject.tableManagement") },
  { key: "terms", label: t("subject.termManagement") },
]);

const isDirty = computed(() => {
  if (!subject.value) return false;
  const s = subject.value;
  return (
    formData.value.name !== s.name ||
    (formData.value.description ?? "") !== (s.description ?? "") ||
    JSON.stringify(formData.value.aliases ?? []) !== JSON.stringify(s.aliases ?? [])
  );
});

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
  newAliasInputVisible.value = true;
  nextTick(() => {
    newAliasInputRef.value?.focus();
  });
};

const handleNewAliasConfirm = () => {
  const value = newAlias.value.trim();
  if (value && !formData.value.aliases?.includes(value)) {
    if (!formData.value.aliases) {
      formData.value.aliases = [];
    }
    formData.value.aliases.push(value);
  }
  newAlias.value = "";
  newAliasInputVisible.value = false;
};

const removeAlias = (alias: string) => {
  formData.value.aliases = formData.value.aliases?.filter((a) => a !== alias) || [];
};

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

const handleCopy = async (text?: string) => {
  if (!text) return;
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(t("mcpService.copySuccess"));
  } catch {
    ElMessage.error(t("mcpService.copyFailed"));
  }
};

onMounted(() => {
  loadSubject();
});
</script>

<template>
  <div v-loading="loading" class="subject-detail-page flex flex-col gap-5 p-6">
    <!-- Top Navigation Header -->
    <div class="detail-header flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-3 flex-wrap">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/subjects' }">
            {{ t("subject.title") }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ subject?.name || subjectId }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <!-- Top Horizontal Underline Tabs -->
    <div class="tabs-bar flex items-center border-b border-[var(--ep-border-color-lighter)] gap-8">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item py-3 px-1 relative text-sm font-medium transition-colors flex items-center gap-2 cursor-pointer border-none bg-transparent"
        :class="
          activeTab === tab.key
            ? 'text-[var(--ep-color-primary)] font-semibold'
            : 'text-[var(--ep-text-color-secondary)] hover:text-[var(--ep-text-color-primary)]'
        "
        @click="activeTab = tab.key"
      >
        <span>{{ tab.label }}</span>

        <!-- Active Underline Indicator -->
        <span
          v-if="activeTab === tab.key"
          class="absolute bottom-0 left-0 right-0 h-0.5 bg-[var(--ep-color-primary)] rounded-full"
        />
      </button>
    </div>

    <!-- Tab Content Container -->
    <main class="main-content flex-1 min-w-0">
      <!-- Tab 1: Basic Info (Single Panel Container) -->
      <div v-if="activeTab === 'basic'" class="panel p-6 shadow-sm flex flex-col gap-6 rounded-xl border border-[var(--ep-border-color-lighter)] bg-[var(--ep-bg-color)]">
        <!-- Panel Header -->
        <div class="flex items-center justify-between border-b border-[var(--ep-border-color-lighter)] pb-4">
          <div>
            <h2 class="text-base font-semibold text-[var(--ep-text-color-primary)] m-0">
              {{ t("subject.basicInfo") }}
            </h2>
            <span class="text-xs text-[var(--ep-text-color-secondary)]">查看与修改数据主题的基础属性及关联配置</span>
          </div>
          <el-button
            type="primary"
            :loading="saving"
            :disabled="!isDirty"
            @click="handleSave"
          >
            {{ t("common.save") }}
          </el-button>
        </div>

        <!-- Form Fields -->
        <el-form label-position="top" class="subject-form flex flex-col gap-5">
          <!-- Readonly ID Row -->
          <div class="flex items-center justify-between p-3 rounded-lg bg-[var(--ep-fill-color-lighter)] text-xs border border-[var(--ep-border-color-lighter)]">
            <div class="flex items-center gap-2">
              <span class="text-[var(--ep-text-color-secondary)] font-medium">主题 ID</span>
              <span class="font-mono text-xs text-[var(--ep-text-color-primary)]">{{ subject?.id || '-' }}</span>
            </div>
            <el-button v-if="subject?.id" link :icon="DocumentCopy" class="!p-0 !h-auto text-[var(--ep-text-color-secondary)] hover:text-[var(--ep-color-primary)]" @click="handleCopy(subject.id)">
              {{ t('common.copy') }}
            </el-button>
          </div>

          <el-form-item :label="t('common.name')" required class="!mb-0">
            <el-input v-model="formData.name" :placeholder="t('common.placeholder.name')" />
          </el-form-item>

          <el-form-item :label="t('common.aliases')" class="!mb-0">
            <div class="flex gap-2 flex-wrap items-center">
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
                class="w-28"
                size="small"
                @keyup.enter="handleNewAliasConfirm"
                @blur="handleNewAliasConfirm"
              />
              <el-button v-else size="small" class="!px-2.5" @click="showNewAliasInput">
                + {{ t('common.aliases') }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item :label="t('common.description')" class="!mb-0">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="3"
              :placeholder="t('common.placeholder.description')"
            />
          </el-form-item>

          <el-form-item :label="t('subject.datasource')" class="!mb-0">
            <div class="flex items-center gap-2 p-3 rounded-lg bg-[var(--ep-fill-color-lighter)] text-xs border border-[var(--ep-border-color-lighter)] w-full">
              <el-icon class="text-[var(--ep-color-primary)]"><Connection /></el-icon>
              <span class="font-semibold text-[var(--ep-text-color-primary)]">{{ subject?.datasource_name || subject?.datasource_id || '-' }}</span>
              <span v-if="subject?.datasource_id" class="font-mono text-[var(--ep-text-color-secondary)] text-[11px] ml-auto">ID: {{ subject.datasource_id }}</span>
            </div>
          </el-form-item>
        </el-form>

        <!-- Panel Footer -->
        <div class="flex items-center justify-between border-t border-[var(--ep-border-color-lighter)] pt-4 text-xs text-[var(--ep-text-color-secondary)]">
          <span>最后修改于：{{ subject?.updated_at ? formatDateTime(subject.updated_at) : '-' }}</span>
          <span v-if="isDirty" class="text-amber-500 font-medium">存在未保存的修改</span>
        </div>
      </div>

      <!-- Tab 2: Table Management -->
      <div v-else-if="activeTab === 'tables'" class="scope-panel p-[20px] shadow-sm">
        <SubjectTableList
          v-if="subject?.datasource_id"
          :subject-id="subjectId"
          :datasource-id="subject.datasource_id"
          @refresh="loadSubject"
        />
      </div>

      <!-- Tab 3: Term Management -->
      <div v-else-if="activeTab === 'terms'" class="scope-panel p-[20px] shadow-sm">
        <TermManager :subject-id="subjectId" />
      </div>
    </main>
  </div>
</template>

<style scoped>
.scope-panel {
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 12px;
  background: var(--ep-bg-color);
}
</style>
