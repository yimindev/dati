<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { Plus, DocumentCopy, WarningFilled } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import { formatDateTime } from "~/composables";
import { copyToClipboard } from "~/utils/clipboard";
import { notifyError } from "~/api/http";
import { createApiKey, deleteApiKey, listApiKeys } from "~/api/auth";
import type { ApiKey, ApiKeyCreated } from "~/api/types";
import DataTableShell from "~/components/common/DataTableShell.vue";

const { t } = useI18n();

const loading = ref(false);
const keys = ref<ApiKey[]>([]);

const dialogVisible = ref(false);
const creating = ref(false);
const formRef = ref<FormInstance>();
// Select sentinel for "never expires". el-select cannot render a null option value
// (selected state is indistinguishable from unselected), so 0 is used in the form
// and converted to undefined (→ omitted from JSON → null → expiresAt=null) on submit.
const NEVER_EXPIRES = 0;
const createForm = ref({ name: "", expiresInDays: NEVER_EXPIRES });
const createdKey = ref<ApiKeyCreated | null>(null);

const rules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t("common.required", { name: t("apiKeys.name") }), trigger: "blur" },
    { max: 64, message: t("common.nameLengthError"), trigger: "blur" },
  ],
}));

const createdKeyDialog = computed({
  get: () => createdKey.value !== null,
  set: (visible: boolean) => {
    if (!visible) createdKey.value = null;
  },
});

async function loadKeys() {
  loading.value = true;
  try {
    keys.value = await listApiKeys();
  } catch (error) {
    console.error("Failed to load API keys:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createForm.value = { name: "", expiresInDays: NEVER_EXPIRES };
  createdKey.value = null;
  dialogVisible.value = true;
}

async function submitCreate() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    creating.value = true;
    try {
      const expiresInDays = createForm.value.expiresInDays === NEVER_EXPIRES ? undefined : createForm.value.expiresInDays;
      createdKey.value = await createApiKey(
        createForm.value.name.trim(),
        expiresInDays
      );
      dialogVisible.value = false;
      await loadKeys();
    } catch (error: any) {
      console.error("Failed to create API key:", error);
      notifyError(error, t("common.operationFailed"));
    } finally {
      creating.value = false;
    }
  });
}

async function removeKey(key: ApiKey) {
  try {
    await ElMessageBox.confirm(
      t("apiKeys.deleteConfirm", { name: key.name }),
      t("apiKeys.deleteTitle"),
      {
        type: "warning",
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
      }
    );
    await deleteApiKey(key.id);
    ElMessage.success(t("apiKeys.deleted"));
    await loadKeys();
  } catch (error: any) {
    console.error("Failed to delete API key:", error);
    notifyError(error, t("common.operationFailed"));
  }
}

async function copyText(text: string) {
  const success = await copyToClipboard(text);
  if (success) {
    ElMessage.success(t("common.copySuccess"));
  } else {
    ElMessage.error(t("common.copyFailed"));
  }
}

function isExpired(expiresAt: string | null): boolean {
  if (!expiresAt) return false;
  return new Date(expiresAt).getTime() < Date.now();
}

onMounted(loadKeys);
</script>

<template>
  <div class="list-page">
    <!-- Page Heading -->
    <div class="page-heading">
      <div>
        <h1 class="text-2xl font-semibold">{{ t("apiKeys.title") }}</h1>
        <p>{{ t("apiKeys.subtitle") }}</p>
      </div>
      <div class="heading-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">
          {{ t("apiKeys.create") }}
        </el-button>
      </div>
    </div>

    <!-- Data Table Shell -->
    <DataTableShell :total="keys.length" :page="1" :page-size="100">
      <el-table v-loading="loading" :data="keys" class="w-full" stripe>
        <el-table-column prop="name" :label="t('apiKeys.name')" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-[var(--ep-text-color-primary)]">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="key_mask" :label="t('apiKeys.key')" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ row.key_mask }}</span>
          </template>
        </el-table-column>

        <el-table-column :label="t('apiKeys.createdAt')" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>

        <el-table-column :label="t('apiKeys.lastUsed')" min-width="160">
          <template #default="{ row }">
            {{ row.last_used_at ? formatDateTime(row.last_used_at) : "-" }}
          </template>
        </el-table-column>

        <el-table-column :label="t('apiKeys.expiresAt')" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="row.expires_at === null" type="info" size="small" effect="plain">
              {{ t("apiKeys.never") }}
            </el-tag>
            <el-tag v-else-if="isExpired(row.expires_at)" type="danger" size="small" effect="light">
              {{ t("apiKeys.expired") }}
            </el-tag>
            <template v-else>
              {{ formatDateTime(row.expires_at) }}
            </template>
          </template>
        </el-table-column>

        <el-table-column :label="t('common.actions')" width="100" align="right" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeKey(row)">
              {{ t("common.delete") }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="t('apiKeys.emptyList')" />
        </template>
      </el-table>
    </DataTableShell>

    <!-- Create API Key Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="t('apiKeys.create')"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="createForm" :rules="rules" label-position="top" @submit.prevent="submitCreate">
        <el-form-item :label="t('apiKeys.name')" prop="name">
          <el-input
            v-model="createForm.name"
            :placeholder="t('common.placeholder.name')"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>

        <el-form-item :label="t('apiKeys.expiresInDaysLabel')">
          <el-select v-model="createForm.expiresInDays" class="w-full">
            <el-option :label="t('apiKeys.never')" :value="NEVER_EXPIRES" />
            <el-option :label="t('apiKeys.days7')" :value="7" />
            <el-option :label="t('apiKeys.days30')" :value="30" />
            <el-option :label="t('apiKeys.days90')" :value="90" />
            <el-option :label="t('apiKeys.days180')" :value="180" />
            <el-option :label="t('apiKeys.days365')" :value="365" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t("common.cancel") }}</el-button>
          <el-button type="primary" :loading="creating" @click="submitCreate">
            {{ t("common.confirm") }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Created Key One-Time Display Dialog -->
    <el-dialog
      v-model="createdKeyDialog"
      :title="t('apiKeys.createdTitle')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :title="t('apiKeys.showOnceWarning')"
        :closable="false"
        show-icon
        class="mb-4"
      />

      <div class="space-y-3">
        <label class="block text-xs font-medium text-[var(--ep-text-color-secondary)]">
          {{ t("apiKeys.key") }}
        </label>
        <el-input :model-value="createdKey?.key" readonly size="large" class="font-mono">
          <template #append>
            <el-button type="primary" :icon="DocumentCopy" @click="copyText(createdKey?.key || '')">
              {{ t("common.copy") }}
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- Usage Guide Preview -->
      <div class="mt-4 rounded-md border border-[var(--ep-border-color-lighter)] p-3 text-xs bg-[var(--ep-fill-color-lighter)]">
        <div class="font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-1.5 mb-1">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ t("apiKeys.usageGuideTitle") }}</span>
        </div>
        <p class="text-[var(--ep-text-color-secondary)] mb-2">
          {{ t("apiKeys.usageGuideDesc") }}
        </p>
        <pre class="m-0 font-mono p-2 rounded bg-[var(--ep-fill-color-light)] text-[var(--ep-text-color-primary)] overflow-x-auto select-all">Authorization: Bearer {{ createdKey?.key }}</pre>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="createdKey = null">
            {{ t("common.confirm") }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

