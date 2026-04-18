<route lang="yaml">
meta:
  activeMenu: /datasources
</route>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { InputInstance } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";
import {
  listTableColumns,
  saveColumnMetadata,
  extractColumnValues,
  getColumnValues,
  saveColumnValues,
  type TableColumnVO,
  type ColumnValueVO,
} from "~/api/column";
import { useSystemStore } from "~/stores/system";
import { formatDateTime } from "~/composables";

const { t } = useI18n();
const systemStore = useSystemStore();
const route = useRoute("/datasources/[id]/tables/[tableId]/columns");

const datasourceId = ref(route.params.id);
const tableId = computed(() => route.params.tableId);

const searchKeyword = ref("");
const loading = ref(false);

const columnList = ref<TableColumnVO[]>([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 弹窗状态（列元数据配置）
const metadataDialogVisible = ref(false);
const currentColumn = ref<TableColumnVO | null>(null);

// 值列表弹窗
type EditableColumnValue = ColumnValueVO & {
  _isDraft?: boolean;
};

const valuesDialogVisible = ref(false);
const columnValues = ref<EditableColumnValue[]>([]);
const valuesLoading = ref(false);
const valueSearchKeyword = ref("");
const valuesPage = ref(1);
const valuesPageSize = ref(10);
const valuesTotal = ref(0);
const valuePageSizes = [10, 20, 50, 100];
const pendingDeletedIds = ref<Set<string>>(new Set());
const dirtyValueMap = ref<Map<string, EditableColumnValue>>(new Map());
const createdValues = ref<EditableColumnValue[]>([]);

// 抽取确认弹框
const extractDialogVisible = ref(false);
const draftValue = ref<EditableColumnValue | null>(null);
const togglingColumnIds = ref<Set<string>>(new Set());
const extractingValues = ref(false);
const savingValues = ref(false);
const valuesQuerySeq = ref(0);
const searchDebounceTimer = ref<ReturnType<typeof setTimeout> | null>(null);
const draftValueInputRef = ref<InputInstance>();
const setDraftValueInputRef = (el: Element | ComponentPublicInstance | null) => {
  draftValueInputRef.value = (el as InputInstance | null) ?? undefined;
};

const maxValueLength = computed(() => systemStore.columnValueLengthLimit ?? 256);
const normalizeForCompare = (value: string) => value.trim().toLocaleLowerCase();

// 动态计算表格最大高度：默认显示10行，超过则滚动
const tableMaxHeight = computed(() => {
  const rowCount = displayedColumnValues.value.length;
  if (rowCount <= 10) {
    return undefined; // 不限制高度，自然展开
  }
  return 500; // 超过10行时固定高度，出现滚动条
});
const hasDraftRow = computed(() => !!draftValue.value);
const hasUnsavedValueChanges = computed(
  () =>
    hasDraftRow.value ||
    createdValues.value.length > 0 ||
    dirtyValueMap.value.size > 0 ||
    pendingDeletedIds.value.size > 0,
);
const displayedColumnValues = computed(() => {
  const rows: EditableColumnValue[] = [];
  if (draftValue.value) {
    rows.push(draftValue.value);
  }

  rows.push(
    ...createdValues.value.filter(
      (item) => !pendingDeletedIds.value.has(item.id),
    ),
  );
  rows.push(
    ...columnValues.value.filter((item) =>
      !pendingDeletedIds.value.has(item.id),
    ),
  );
  return rows;
});

const cloneEditableValue = (item: EditableColumnValue): EditableColumnValue => ({
  id: item.id,
  value: item.value,
  synonyms: [...(item.synonyms || [])],
  _editing: false,
  _synonymInput: "",
  _isDraft: item._isDraft,
});

const isSameValueRow = (
  left: EditableColumnValue,
  right?: EditableColumnValue,
): boolean => {
  if (!right) return false;
  if (left === right) return true;
  if (left.id && right.id) return left.id === right.id;
  return false;
};

const markDirtyValue = (row: EditableColumnValue) => {
  if (!row.id || row._isDraft || createdValues.value.includes(row)) return;
  dirtyValueMap.value.set(row.id, cloneEditableValue(row));
};

const resetValueDialogState = () => {
  valuesPage.value = 1;
  valuesPageSize.value = 10;
  valuesTotal.value = 0;
  valueSearchKeyword.value = "";
  columnValues.value = [];
  draftValue.value = null;
  pendingDeletedIds.value.clear();
  dirtyValueMap.value.clear();
  createdValues.value = [];
};

const loadValues = async () => {
  if (!currentColumn.value?.id) return;

  const queryId = ++valuesQuerySeq.value;
  valuesLoading.value = true;
  try {
    const resp = await getColumnValues(
      datasourceId.value,
      tableId.value,
      currentColumn.value.id,
      valuesPage.value,
      valuesPageSize.value,
      valueSearchKeyword.value || undefined,
    );
    if (queryId !== valuesQuerySeq.value) return;

    valuesTotal.value = resp.total ?? 0;
    valuesPage.value = resp.page ?? valuesPage.value;
    valuesPageSize.value = resp.size ?? valuesPageSize.value;

    columnValues.value = (resp.data || [])
      .filter((item) => !pendingDeletedIds.value.has(item.id))
      .map((item) => {
        const dirty = dirtyValueMap.value.get(item.id);
        return cloneEditableValue(dirty ?? item);
      });

    valuesDialogVisible.value = true;
  } catch (error) {
    console.error("加载列值失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    if (queryId === valuesQuerySeq.value) {
      valuesLoading.value = false;
    }
  }
};

// 判断列类型是否支持值匹配（只支持字符串类型）
const isValueMatchingSupported = (columnType?: string): boolean => {
  if (!columnType) return false;
  const lowerType = columnType.toLowerCase();
  return lowerType.includes('varchar') || 
         lowerType.includes('char') || 
         lowerType.includes('text') ||
         lowerType.includes('string');
};

// 值匹配开关二次确认
const handleBeforeToggle = (row: TableColumnVO | null): Promise<boolean> => {
  if (!row) return Promise.resolve(false);
  if (row.extract_value_enabled) {
    return ElMessageBox.confirm(
      t('column.disableValueMatchingConfirm'),
      t('common.warning'),
      {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      }
    ).then(() => true);
  }
  return Promise.resolve(true);
};

// 行内切换值匹配开关
const handleToggleValueMatching = async (row: TableColumnVO, newValue: string | number | boolean) => {
  if (!row.id || togglingColumnIds.value.has(row.id)) return;

  togglingColumnIds.value.add(row.id);
  try {
    await saveColumnMetadata(datasourceId.value, tableId.value, {
      ...row,
      extract_value_enabled: Boolean(newValue)
    });
    ElMessage.success(newValue ? t('column.valueMatchingEnabled') : t('column.valueMatchingDisabled'));
  } catch (error) {
    row.extract_value_enabled = !newValue;
    console.error('保存值匹配状态失败:', error);
    ElMessage.error(t('common.operationFailed'));
  } finally {
    togglingColumnIds.value.delete(row.id);
  }
};

// 打开管理值弹窗（从行内）
const handleManageValues = async (row: TableColumnVO) => {
  currentColumn.value = { ...row, aliases: row.aliases || [] };
  await handleOpenValuesDialog();
};

const loadColumns = async () => {
  try {
    loading.value = true;
    const resp = await listTableColumns(
      datasourceId.value,
      tableId.value,
      page.value,
      pageSize.value,
      searchKeyword.value,
    );

    columnList.value = resp.data || [];
    total.value = resp.total ?? 0;
  } catch (error) {
    console.error("加载列信息失败:", error);
    ElMessage.error(t("common.loadFailed"));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  loadColumns();
};

const handleClearSearch = () => {
  searchKeyword.value = "";
  page.value = 1;
  loadColumns();
};

const handlePageChange = (p: number) => {
  page.value = p;
  loadColumns();
};

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps;
  page.value = 1;
  loadColumns();
};

const handleConfigMetadata = (col: TableColumnVO) => {
  currentColumn.value = { ...col, aliases: col.aliases || [] };
  metadataDialogVisible.value = true;
};

// 别名管理
const newAlias = ref("");
const newAliasInputVisible = ref(false);
const newAliasInputRef = ref<InputInstance>();

const handleNewAliasConfirm = () => {
  if (!currentColumn.value || !newAlias.value.trim()) return;
  if (!currentColumn.value.aliases) {
    currentColumn.value.aliases = [];
  }
  if (!currentColumn.value.aliases.includes(newAlias.value.trim())) {
    currentColumn.value.aliases.push(newAlias.value.trim());
  }
  newAliasInputVisible.value = false;
  newAlias.value = "";
};

const showNewAliasInput = () => {
  newAliasInputVisible.value = true;
  nextTick(() => {
    newAliasInputRef.value?.focus();
  });
};

const removeAlias = (alias: string) => {
  if (!currentColumn.value?.aliases) return;
  currentColumn.value.aliases = currentColumn.value.aliases.filter(
    (a) => a !== alias,
  );
};

const handleSaveMetadata = async () => {
  try {
    await saveColumnMetadata(
      datasourceId.value,
      tableId.value,
      currentColumn.value!,
    );
    ElMessage.success(t("common.saveSuccess"));
    metadataDialogVisible.value = false;
    await loadColumns();
  } catch (error) {
    console.error("保存列元数据失败:", error);
    ElMessage.error(t("common.operationFailed"));
  }
};

// ========== 值抽取相关 ==========

const handleExtractValues = async () => {
  if (!currentColumn.value?.id || extractingValues.value) return;
  if (hasUnsavedValueChanges.value) {
    ElMessage.warning(t("column.saveBeforeExtract"));
    return;
  }

  extractDialogVisible.value = true;
};

const handleConfirmExtract = async (overwrite: boolean) => {
  extractDialogVisible.value = false;

  if (!currentColumn.value?.id) return;

  // 覆盖操作需要二次确认
  if (overwrite) {
    try {
      await ElMessageBox.confirm(
        t('column.extractOverwriteConfirm'),
        t('common.warning'),
        { type: 'warning' }
      );
    } catch {
      return;
    }
  }

  try {
    extractingValues.value = true;
    await extractColumnValues(
      datasourceId.value,
      tableId.value,
      currentColumn.value.id,
      overwrite,
    );
    ElMessage.success(t("column.extractSuccess"));
    valuesPage.value = 1;
    await loadValues();
  } catch (error) {
    console.error("抽取列值失败:", error);
    ElMessage.error(t("column.extractFailed"));
  } finally {
    extractingValues.value = false;
  }
};

const handleOpenValuesDialog = async () => {
  if (!currentColumn.value?.id) return;

  resetValueDialogState();
  await loadValues();
};

// ========== 值列表管理 ==========

const validateColumnValue = (
  rawValue: string,
  excludeRow?: EditableColumnValue,
): string | null => {
  const normalizedInput = rawValue.trim();
  if (!normalizedInput) {
    return t("column.valueRequired");
  }
  if (normalizedInput.length > maxValueLength.value) {
    return t("column.valueLengthExceeded", { max: maxValueLength.value });
  }
  const candidates: EditableColumnValue[] = [];
  if (draftValue.value) {
    candidates.push(draftValue.value);
  }
  candidates.push(...createdValues.value, ...columnValues.value, ...dirtyValueMap.value.values());
  const duplicate = candidates.some(
    (item) =>
      !isSameValueRow(item, excludeRow) &&
      !pendingDeletedIds.value.has(item.id) &&
      normalizeForCompare(item.value) === normalizeForCompare(normalizedInput),
  );
  if (duplicate) {
    return t("column.valueDuplicate");
  }
  return null;
};

const focusDraftValueInput = () => {
  nextTick(() => {
    draftValueInputRef.value?.focus();
  });
};

const handleAddDraftValue = () => {
  if (draftValue.value) {
    focusDraftValueInput();
    return;
  }

  draftValue.value = {
    id: "",
    value: "",
    synonyms: [],
    _editing: false,
    _synonymInput: "",
    _isDraft: true,
  };
  focusDraftValueInput();
};

const handleConfirmDraftValue = (row: EditableColumnValue) => {
  if (!row._isDraft) return;

  const validationError = validateColumnValue(row.value, row);
  if (validationError) {
    ElMessage.warning(validationError);
    focusDraftValueInput();
    return;
  }

  row.value = row.value.trim();
  createdValues.value.unshift({
    ...cloneEditableValue(row),
    _isDraft: false,
  });
  draftValue.value = null;
};

const handleCancelDraftValue = () => {
  draftValue.value = null;
};

const handleDeleteValue = (row: EditableColumnValue) => {
  if (row._isDraft) {
    handleCancelDraftValue();
    return;
  }
  const createdIndex = createdValues.value.indexOf(row);
  if (createdIndex >= 0) {
    createdValues.value.splice(createdIndex, 1);
    return;
  }
  if (row.id) {
    pendingDeletedIds.value.add(row.id);
    dirtyValueMap.value.delete(row.id);
    columnValues.value = columnValues.value.filter((item) => item.id !== row.id);
    return;
  }
};

// 同义词编辑
const handleSynonymConfirm = (row: EditableColumnValue) => {
  const normalizedInput = row._synonymInput?.trim() ?? "";
  if (!normalizedInput) {
    row._editing = false;
    return;
  }
  if (normalizedInput.length > maxValueLength.value) {
    ElMessage.warning(t("column.valueLengthExceeded", { max: maxValueLength.value }));
    row._editing = false;
    row._synonymInput = "";
    return;
  }

  if (!row.synonyms) {
    row.synonyms = [];
  }

  const duplicatedSynonym = row.synonyms.some(
    (item) => normalizeForCompare(item) === normalizeForCompare(normalizedInput),
  );
  if (duplicatedSynonym) {
    ElMessage.warning(t("column.synonymDuplicate"));
    row._editing = false;
    row._synonymInput = "";
    return;
  }
  row.synonyms.push(normalizedInput);
  markDirtyValue(row);

  row._synonymInput = "";
  row._editing = false;
};

const showSynonymInput = (row: EditableColumnValue) => {
  row._editing = true;
  row._synonymInput = "";
};

const removeSynonym = (row: EditableColumnValue, syn: string) => {
  if (!row.synonyms) return;
  row.synonyms = row.synonyms.filter((s) => s !== syn);
  markDirtyValue(row);
};

const handleSaveValues = async () => {
  if (!currentColumn.value?.id || savingValues.value) return;
  if (hasDraftRow.value) {
    ElMessage.warning(t("column.finishDraftBeforeSave"));
    focusDraftValueInput();
    return;
  }

  try {
    savingValues.value = true;
    const changedValues = [
      ...Array.from(dirtyValueMap.value.values()),
      ...createdValues.value,
    ];
    await saveColumnValues(
      datasourceId.value,
      tableId.value,
      currentColumn.value.id,
      changedValues,
      Array.from(pendingDeletedIds.value),
    );

    ElMessage.success(t("common.saveSuccess"));
    valuesDialogVisible.value = false;
    resetValueDialogState();
  } catch (error) {
    console.error("保存列值失败:", error);
    ElMessage.error(t("common.operationFailed"));
  } finally {
    savingValues.value = false;
  }
};

const handleValuePageChange = async (nextPage: number) => {
  if (hasDraftRow.value) {
    ElMessage.warning(t("column.finishDraftBeforeAction"));
    focusDraftValueInput();
    return;
  }
  valuesPage.value = nextPage;
  await loadValues();
};

const handleValuePageSizeChange = async (nextSize: number) => {
  if (hasDraftRow.value) {
    ElMessage.warning(t("column.finishDraftBeforeAction"));
    focusDraftValueInput();
    return;
  }
  valuesPage.value = 1;
  valuesPageSize.value = nextSize;
  await loadValues();
};

watch(valueSearchKeyword, () => {
  if (!valuesDialogVisible.value) return;
  if (searchDebounceTimer.value) {
    clearTimeout(searchDebounceTimer.value);
  }
  searchDebounceTimer.value = setTimeout(() => {
    if (hasDraftRow.value) return;
    valuesPage.value = 1;
    loadValues();
  }, 300);
});

onBeforeUnmount(() => {
  if (searchDebounceTimer.value) {
    clearTimeout(searchDebounceTimer.value);
    searchDebounceTimer.value = null;
  }
});

onMounted(() => {
  loadColumns();
});
</script>

<template>
  <div class="p-5 md:p-6">
    <el-breadcrumb separator="/" class="mb-6">
      <el-breadcrumb-item :to="{ path: '/datasources' }">
        {{ t("layout.side.dataSources") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item
        :to="{ path: `/datasources/${datasourceId}/tables` }"
      >
        {{ t("tableInfo.title") }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>
        {{ t("tableInfo.columnSettings") }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 页面头部 -->
    <div class="flex items-center justify-between gap-4 mb-6">
      <el-input
        v-model="searchKeyword"
        :placeholder="t('column.searchPlaceholder')"
        clearable
        class="max-w-sm"
        @keyup.enter="handleSearch"
        @clear="handleClearSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" />
        </template>
      </el-input>
    </div>

    <!-- 列表 -->
    <el-table :data="columnList" v-loading="loading" stripe>
      <el-table-column
        prop="name"
        :label="t('column.columnName')"
        min-width="180"
      />
      <el-table-column :label="t('common.aliases')" min-width="180">
        <template #default="{ row }">
          <el-tag
            v-for="alias in row.aliases"
            :key="alias"
            size="small"
            class="mr-1"
          >
            {{ alias }}
          </el-tag>
          <span v-if="!row.aliases?.length" class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="description"
        :label="t('column.description')"
        min-width="160"
      />
      <el-table-column
        prop="column_type"
        :label="t('column.type')"
        min-width="120"
      />

      <!-- 值匹配开关 -->
      <el-table-column
        :label="t('column.valueMatching')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-switch
            v-if="isValueMatchingSupported(row.column_type)"
            v-model="row.extract_value_enabled"
            :loading="!!(row.id && togglingColumnIds.has(row.id))"
            :disabled="!!(row.id && togglingColumnIds.has(row.id))"
            :before-change="() => handleBeforeToggle(row)"
            @change="(val: string | number | boolean) => handleToggleValueMatching(row, val)"
          />
          <span v-else class="text-gray-300">-</span>
        </template>
      </el-table-column>

      <el-table-column
        prop="updated_at"
        :label="t('common.updatedAt')"
        min-width="140"
      >
        <template #default="{ row }">
          {{ row.updated_at ? formatDateTime(row.updated_at) : "-" }}
        </template>
      </el-table-column>

      <el-table-column
        :label="t('common.actions')"
        width="180"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="handleConfigMetadata(row)"
          >
            {{ t("common.edit") }}
          </el-button>
          <el-button
            v-if="row.extract_value_enabled && isValueMatchingSupported(row.column_type)"
            link
            type="primary"
            @click="handleManageValues(row)"
          >
            {{ t("column.manageValues") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="flex items-center justify-between mt-4">
      <span class="text-gray-500 text-sm">{{
        t("common.total", { total })
      }}</span>
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

    <!-- 列元数据配置弹窗 -->
    <el-dialog
      v-model="metadataDialogVisible"
      :title="t('column.editTitle')"
      width="600px"
    >
      <el-form
        v-if="currentColumn"
        :model="currentColumn"
        label-width="120px"
      >
        <el-form-item :label="t('column.columnName')">
          <el-input v-model="currentColumn.name" disabled />
        </el-form-item>

        <el-form-item :label="t('common.aliases')">
          <div class="flex gap-2 flex-wrap">
            <el-tag
              v-for="alias in currentColumn.aliases"
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
              + {{ t("common.aliases") }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item :label="t('column.description')">
          <el-input
            v-model="currentColumn.description"
            :placeholder="t('column.enterDescription')"
          />
        </el-form-item>

        <el-form-item :label="t('column.valueMatching')">
          <el-switch
            v-if="isValueMatchingSupported(currentColumn.column_type)"
            v-model="currentColumn.extract_value_enabled"
            :before-change="() => handleBeforeToggle(currentColumn)"
          />
          <span v-else class="text-gray-400 text-sm">
            {{ t('column.valueMatchingUnsupported') }}
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="metadataDialogVisible = false">
          {{ t("common.cancel") }}
        </el-button>
        <el-button type="primary" @click="handleSaveMetadata">
          {{ t("common.save") }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 管理值弹窗 -->
    <el-dialog
      v-model="valuesDialogVisible"
      :title="currentColumn ? t('column.manageValuesTitle', { columnName: currentColumn.name }) : t('column.manageValues')"
      width="800px"
      :close-on-click-modal="false"
      class="values-dialog"
    >
      <div v-loading="valuesLoading" class="values-dialog-content">
        <!-- 操作栏：搜索 + 抽取 + 添加 -->
        <div class="mb-4 flex items-center justify-between">
          <el-input
            v-model="valueSearchKeyword"
            clearable
            class="max-w-sm"
            :placeholder="t('column.valueSearchPlaceholder')"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div class="flex items-center gap-2">
            <el-button
              type="primary"
              :loading="extractingValues"
              :disabled="valuesLoading || savingValues || hasDraftRow"
              @click="handleExtractValues"
            >
              {{ t('column.extractButton') }}
            </el-button>
            <el-button
              :disabled="valuesLoading || savingValues || extractingValues"
              @click="handleAddDraftValue"
            >
              {{ t("column.manualAdd") }}
            </el-button>
          </div>
        </div>

        <div class="mb-3 text-gray-500 text-sm">
          {{ t('column.sampleLimitHint', { limit: systemStore.columnValueSampleLimit }) }}
        </div>

        <!-- 值列表表格 -->
        <el-table :data="displayedColumnValues" stripe style="width: 100%" :max-height="tableMaxHeight">
          <el-table-column :label="t('column.valueLabel')" min-width="180" show-overflow-tooltip>
            <template #default="{ row, $index }">
              <div class="flex items-center gap-2">
                <el-tag v-if="row._isDraft" size="small" type="success">
                  {{ t("column.newValueTag") }}
                </el-tag>
                <el-input
                  v-if="row._isDraft"
                  :ref="$index === 0 ? setDraftValueInputRef : undefined"
                  v-model="row.value"
                  :placeholder="t('column.enterValue')"
                  @keyup.enter="handleConfirmDraftValue(row)"
                  @keyup.esc="handleCancelDraftValue"
                />
                <span v-else>{{ row.value }}</span>
              </div>
            </template>
          </el-table-column>

          <!-- 同义词列（行内编辑） -->
          <el-table-column :label="t('column.synonyms')" min-width="220">
            <template #default="{ row }">
              <div class="flex flex-wrap gap-1 items-center">
                <el-tag
                  v-for="syn in row.synonyms"
                  :key="syn"
                  size="small"
                  closable
                  @close="removeSynonym(row, syn)"
                >
                  {{ syn }}
                </el-tag>

                <!-- 同义词输入框 -->
                <el-input
                  v-if="row._editing"
                  v-model="row._synonymInput"
                  class="w-24"
                  size="small"
                  :placeholder="t('column.synonymPlaceholder')"
                  @keyup.enter="handleSynonymConfirm(row)"
                  @blur="handleSynonymConfirm(row)"
                />
                <el-button
                  v-else
                  link
                  size="small"
                  @click="showSynonymInput(row)"
                >
                  + {{ t("common.add") }}
                </el-button>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            :label="t('common.actions')"
            width="130"
            fixed="right"
          >
            <template #default="{ row }">
              <template v-if="row._isDraft">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleConfirmDraftValue(row)"
                >
                  {{ t("common.confirm") }}
                </el-button>
                <el-button
                  link
                  size="small"
                  @click="handleCancelDraftValue"
                >
                  {{ t("common.cancel") }}
                </el-button>
              </template>
              <el-button
                v-else
                link
                type="danger"
                size="small"
                @click="handleDeleteValue(row)"
              >
                {{ t("common.delete") }}
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="text-center text-gray-400 py-8">
              {{ valueSearchKeyword ? t("column.noValueSearchResult") : t("column.noValues") }}
            </div>
          </template>
        </el-table>

        <div v-if="valuesTotal > 0" class="flex items-center justify-between mt-4">
          <span class="text-gray-500 text-sm">{{ t('common.total', { total: valuesTotal }) }}</span>
          <el-pagination
            layout="sizes, prev, pager, next"
            :current-page="valuesPage"
            :page-size="valuesPageSize"
            :page-sizes="valuePageSizes"
            :total="valuesTotal"
            @current-change="handleValuePageChange"
            @size-change="handleValuePageSizeChange"
          />
        </div>
      </div>

      <template #footer>
        <el-button :disabled="savingValues || extractingValues" @click="valuesDialogVisible = false">
          {{ t("common.cancel") }}
        </el-button>
        <el-button type="primary" :loading="savingValues" @click="handleSaveValues">
          {{ t("common.save") }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 抽取确认弹框 -->
    <el-dialog
      v-model="extractDialogVisible"
      :title="t('column.extractConfirmTitle')"
      width="400px"
      :close-on-click-modal="false"
    >
      <p class="text-gray-600 mb-6">{{ t('column.extractConfirmMessage') }}</p>
      <div class="flex gap-3">
        <el-button class="flex-1" @click="handleConfirmExtract(true)">
          {{ t('column.extractOverwrite') }}
        </el-button>
        <el-button type="primary" class="flex-1" @click="handleConfirmExtract(false)">
          {{ t('column.extractAppend') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>
