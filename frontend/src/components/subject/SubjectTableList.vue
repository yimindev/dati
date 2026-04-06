<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import type { TableInfoVO, SubjectAvailableTableVO } from "~/api/subject";
import { getSubjectTables, getAvailableTables, addTableToSubject, removeTableFromSubject } from "~/api/subject";
import { getSchemas } from "~/api/datasource";

const { t } = useI18n();

interface Props {
  subjectId: string;
  datasourceId: string;
}

const props = defineProps<Props>();

const loading = ref(false);
const tableList = ref<TableInfoVO[]>([]);

const addTableDialogVisible = ref(false);
const schemas = ref<string[]>([]);
const selectedSchema = ref("");
const availableTables = ref<SubjectAvailableTableVO[]>([]);
const selectedTableIds = ref<string[]>([]);
const schemaLoading = ref(false);
const tablesLoading = ref(false);
const addTableLoading = ref(false);

interface TransferItem {
  key: string;
  label: string;
  disabled: boolean;
}

const linkedTableIds = computed(() => {
  return tableList.value.map(t => t.id);
});

const transferData = computed<TransferItem[]>(() => {
  return availableTables.value.map(table => ({
    key: table.table_id,
    label: table.table_name,
    disabled: linkedTableIds.value.includes(table.table_id),
  }));
});

const loadTables = async () => {
  try {
    loading.value = true;
    tableList.value = await getSubjectTables(props.subjectId);
  } catch (error) {
    console.error("Failed to load subject tables:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleOpenAddTableDialog = async () => {
  addTableDialogVisible.value = true;
  selectedSchema.value = "";
  availableTables.value = [];
  selectedTableIds.value = [];

  try {
    schemaLoading.value = true;
    schemas.value = await getSchemas(props.datasourceId);
  } catch (error) {
    console.error("Failed to load schemas:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    schemaLoading.value = false;
  }
};

const handleSchemaChange = async (schema: string) => {
  if (!schema) {
    availableTables.value = [];
    return;
  }

  try {
    tablesLoading.value = true;
    availableTables.value = await getAvailableTables(props.subjectId, schema);
    selectedTableIds.value = [];
  } catch (error) {
    console.error("Failed to load tables:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    tablesLoading.value = false;
  }
};

const handleBatchAdd = async () => {
  if (selectedTableIds.value.length === 0) {
    ElMessage.warning(t("tableInfo.selectAtLeastOne"));
    return;
  }

  try {
    addTableLoading.value = true;
    for (const tableId of selectedTableIds.value) {
      await addTableToSubject(props.subjectId, { table_id: tableId });
    }
    ElMessage.success(t("subject.addTableSuccess"));
    addTableDialogVisible.value = false;
    await loadTables();
  } catch (error) {
    console.error("Failed to add tables:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    addTableLoading.value = false;
  }
};

const handleRemoveTable = async (table: TableInfoVO) => {
  try {
    await ElMessageBox.confirm(
      t("subject.removeTableConfirm", { name: table.name }),
      t("common.warning"),
      {
        confirmButtonText: t("common.confirm"),
        cancelButtonText: t("common.cancel"),
        type: "warning",
      }
    );
    await removeTableFromSubject(props.subjectId, table.id);
    ElMessage.success(t("subject.removeTableSuccess"));
    await loadTables();
  } catch (error) {
    if (error !== "cancel") {
      console.error("Failed to remove table:", error);
      ElMessage.error(t("common.operationFailed"));
    }
  }
};

onMounted(() => {
  loadTables();
});
</script>

<template>
  <div class="subject-table-list">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-medium">{{ t("subject.tableManagement") }}</h3>
      <el-button type="primary" :icon="Plus" @click="handleOpenAddTableDialog">
        {{ t("subject.addTable") }}
      </el-button>
    </div>

    <el-table :data="tableList" v-loading="loading" stripe>
      <el-table-column
        prop="name"
        :label="t('common.tableName')"
        min-width="150"
      />
      <el-table-column
        prop="display_name"
        :label="t('tableInfo.displayName')"
        min-width="150"
      />
      <el-table-column
        prop="schema"
        :label="t('common.schema')"
        min-width="120"
      />
      <el-table-column
        prop="description"
        :label="t('common.description')"
        min-width="150"
      />
      <el-table-column
        :label="t('common.actions')"
        min-width="100"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button link type="danger" @click="handleRemoveTable(row)">
            {{ t("common.remove") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && tableList.length === 0" :description="t('subject.noTables')" />

    <el-dialog
      v-model="addTableDialogVisible"
      :title="t('subject.addTable')"
      width="700px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item :label="t('common.schema')" required>
          <el-select
            v-model="selectedSchema"
            :placeholder="t('tableInfo.selectSchema')"
            :loading="schemaLoading"
            @change="handleSchemaChange"
          >
            <el-option
              v-for="schema in schemas"
              :key="schema"
              :label="schema"
              :value="schema"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="selectedSchema" v-loading="tablesLoading">
          <el-transfer
            v-model="selectedTableIds"
            :data="transferData"
            filterable
            :filter-placeholder="t('common.search')"
            :titles="[t('tableInfo.availableTables'), t('tableInfo.selectedTables')]"
            :button-texts="['', '']"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="addTableDialogVisible = false">
          {{ t("common.cancel") }}
        </el-button>
        <el-button
          type="primary"
          :loading="addTableLoading"
          :disabled="selectedTableIds.length === 0"
          @click="handleBatchAdd"
        >
          {{ t("tableInfo.addSelected") }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.subject-table-list {
  padding: 16px 0;
}
</style>
