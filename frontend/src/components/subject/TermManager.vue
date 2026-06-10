<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import type { TermVO, TermRelationVO, CreateTermRequest, UpdateTermRequest, LinkTermRelationRequest, TableInfoVO } from '~/api/subject'
import { getTermsBySubject, createTerm, updateTerm, deleteTerm, getTermDetail, linkTermRelation, unlinkTermRelation, getSubjectTables } from '~/api/subject'
import { listTableColumns } from '~/api/column'

const { t } = useI18n()

interface Props {
  subjectId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const termList = ref<TermVO[]>([])
const searchKeyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const termDetails = ref<Map<string, TermVO & { relations: TermRelationVO[] }>>(new Map())

const termDialogVisible = ref(false)
const termDialogLoading = ref(false)
const editingTerm = ref<TermVO | null>(null)
const termDialogTitle = computed(() => editingTerm.value ? t('subject.editTerm') : t('subject.addTerm'))
const termFormRef = ref<FormInstance>()
const termFormData = ref<CreateTermRequest>({ name: '', description: '', aliases: [] })

const relationDialogVisible = ref(false)
const relationDialogLoading = ref(false)
const relationTermId = ref('')

const availableTables = ref<TableInfoVO[]>([])
const tablePage = ref(1)
const tableTotal = ref(0)
const tableLoadingMore = ref(false)
const tableHasMore = computed(() => availableTables.value.length < tableTotal.value)
const selectedTableIds = ref<Set<string>>(new Set())
const tableLevelTableIds = ref<Set<string>>(new Set())
const selectedFieldsByTable = ref<Map<string, Set<string>>>(new Map())
const fieldSearchByTable = ref<Map<string, string>>(new Map())
const activeFieldConfigTableId = ref('')
const tableColumnsData = ref<Map<string, { name: string }[]>>(new Map())
const loadingTableColumns = ref<Set<string>>(new Set())
const tableSearchKeyword = ref('')
const tableRef = ref()

const rules: FormRules = {
  name: [
    { required: true, message: t('common.required', { name: t('common.name') }), trigger: 'blur' },
    { min: 1, max: 100, message: t('common.nameLengthError'), trigger: 'blur' }
  ]
}

const newAlias = ref('')
const newAliasInputVisible = ref(false)
const newAliasInputRef = ref()

const showNewAliasInput = () => {
  newAliasInputVisible.value = true
  nextTick(() => {
    newAliasInputRef.value?.focus()
  })
}

const handleNewAliasConfirm = () => {
  const value = newAlias.value.trim()
  if (value && !termFormData.value.aliases?.includes(value)) {
    if (!termFormData.value.aliases) {
      termFormData.value.aliases = []
    }
    termFormData.value.aliases.push(value)
  }
  newAlias.value = ''
  newAliasInputVisible.value = false
}

const removeTermAlias = (alias: string) => {
  termFormData.value.aliases = termFormData.value.aliases?.filter(a => a !== alias) || []
}

const loadTerms = async () => {
  try {
    loading.value = true
    const keywordParam = searchKeyword.value.trim() || undefined
    const response = await getTermsBySubject(
      props.subjectId,
      page.value,
      pageSize.value,
      keywordParam,
    )
    termList.value = response.data ?? []
    total.value = response.total ?? 0
  } catch (error) {
    console.error('Failed to load terms:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadTerms()
}

const handleClearSearch = () => {
  searchKeyword.value = ''
  page.value = 1
  loadTerms()
}

const handlePageChange = (p: number) => {
  page.value = p
  loadTerms()
}

const handlePageSizeChange = (ps: number) => {
  pageSize.value = ps
  page.value = 1
  loadTerms()
}

const loadTermDetail = async (termId: string) => {
  try {
    const detail = await getTermDetail(termId)
    termDetails.value.set(termId, detail)
  } catch (error) {
    console.error('Failed to load term detail:', error)
  }
}

const getTermRelations = (termId: string) => {
  return termDetails.value.get(termId)?.relations || []
}

const getTermDisplayRelations = (termId: string) => {
  return getTermRelations(termId).slice(0, 2)
}

const getRelationDisplayText = (relation: TermRelationVO) => {
  const tableName = relation.schema ? `${relation.schema}.${relation.table_name}` : relation.table_name
  if (relation.entity_type === 'TABLE') {
    return tableName
  }
  return `${tableName}.${relation.field_name}`
}

const buildRelationKey = (entityType: 'TABLE' | 'FIELD', tableId: string, fieldName?: string) => {
  return entityType === 'TABLE'
    ? `TABLE:${tableId}`
    : `FIELD:${tableId}:${fieldName || ''}`
}

const getExistingRelationKeys = (termId: string) => {
  const keys = new Set<string>()
  for (const relation of getTermRelations(termId)) {
    keys.add(buildRelationKey(relation.entity_type, relation.table_id, relation.field_name))
  }
  return keys
}

const selectedTables = computed(() => {
  return availableTables.value.filter(table => selectedTableIds.value.has(table.id))
})

const selectedRelationCount = computed(() => {
  let count = tableLevelTableIds.value.size
  for (const fields of selectedFieldsByTable.value.values()) {
    count += fields.size
  }
  return count
})

const isTableLevelEnabled = (tableId: string) => tableLevelTableIds.value.has(tableId)

const isTableColumnsLoading = (tableId: string) => loadingTableColumns.value.has(tableId)

const getSelectedFieldList = (tableId: string) => {
  return Array.from(selectedFieldsByTable.value.get(tableId) || [])
}

const getFieldSearch = (tableId: string) => {
  return fieldSearchByTable.value.get(tableId) || ''
}

const setFieldSearch = (tableId: string, keyword: string) => {
  fieldSearchByTable.value.set(tableId, keyword)
}

const getFilteredColumnsByTable = (tableId: string) => {
  const columns = tableColumnsData.value.get(tableId) || []
  const keyword = getFieldSearch(tableId).trim().toLowerCase()
  if (!keyword) return columns
  return columns.filter(col => col.name.toLowerCase().includes(keyword))
}

const getFieldEmptyTextByTable = (tableId: string) => {
  const allColumns = tableColumnsData.value.get(tableId) || []
  const filteredColumns = getFilteredColumnsByTable(tableId)
  if (getFieldSearch(tableId) && filteredColumns.length === 0) return t('subject.noSearchResults')
  if (allColumns.length === 0) return t('subject.noFieldsInTable')
  return ''
}

const resetRelationEditorState = () => {
  selectedTableIds.value = new Set()
  tableLevelTableIds.value = new Set()
  selectedFieldsByTable.value = new Map()
  fieldSearchByTable.value = new Map()
  activeFieldConfigTableId.value = ''
  tableSearchKeyword.value = ''
}

const handleSelectionChange = (rows: TableInfoVO[]) => {
  const newIds = new Set(rows.map(r => r.id))

  for (const row of rows) {
    const id = row.id
    if (!selectedTableIds.value.has(id)) {
      tableLevelTableIds.value.add(id)
      if (!selectedFieldsByTable.value.has(id)) {
        selectedFieldsByTable.value.set(id, new Set())
      }
      if (!activeFieldConfigTableId.value) {
        activeFieldConfigTableId.value = id
      }
      loadColumnsForTable(id)
    }
  }

  for (const id of selectedTableIds.value) {
    if (!newIds.has(id)) {
      tableLevelTableIds.value.delete(id)
      selectedFieldsByTable.value.delete(id)
      fieldSearchByTable.value.delete(id)
      if (activeFieldConfigTableId.value === id) {
        activeFieldConfigTableId.value = rows[0]?.id || ''
      }
    }
  }

  selectedTableIds.value = newIds
}

const handleRowClick = (row: TableInfoVO) => {
  tableRef.value?.toggleRowSelection(row)
}

const handleToggleTableLevel = (tableId: string, enabled: boolean) => {
  if (enabled) {
    tableLevelTableIds.value.add(tableId)
  } else {
    tableLevelTableIds.value.delete(tableId)
  }
}

const handleFieldsChange = (tableId: string, fields: string[]) => {
  selectedFieldsByTable.value.set(tableId, new Set(fields))
  if (fields.length > 0) {
    tableLevelTableIds.value.delete(tableId)
  }
}

const handleFieldConfigTabChange = async (paneName: string | number) => {
  const tableId = String(paneName)
  activeFieldConfigTableId.value = tableId
  await loadColumnsForTable(tableId)
}

const loadTermsDetails = async () => {
  for (const term of termList.value) {
    if (!termDetails.value.has(term.id)) {
      await loadTermDetail(term.id)
    }
  }
}

const loadTermsAndDetails = async () => {
  await loadTerms()
  await loadTermsDetails()
}

const handleOpenTermDialog = (term?: TermVO) => {
  if (term) {
    editingTerm.value = term
    termFormData.value = {
      name: term.name,
      description: term.description || '',
      aliases: term.aliases ? [...term.aliases] : []
    }
  } else {
    editingTerm.value = null
    termFormData.value = { name: '', description: '', aliases: [] }
  }
  termDialogVisible.value = true
}

const handleCloseTermDialog = () => {
  termFormRef.value?.clearValidate()
  termDialogVisible.value = false
}

const handleSubmitTerm = async () => {
  try {
    const valid = await termFormRef.value?.validate()
    if (!valid) return

    termDialogLoading.value = true

    if (editingTerm.value) {
      const updateData: UpdateTermRequest = {
        name: termFormData.value.name,
        description: termFormData.value.description,
        aliases: termFormData.value.aliases
      }
      await updateTerm(editingTerm.value.id, updateData)
      ElMessage.success(t('subject.updateTermSuccess'))
    } else {
      await createTerm(props.subjectId, termFormData.value)
      ElMessage.success(t('subject.addTermSuccess'))
    }

    handleCloseTermDialog()
    await loadTermsAndDetails()
  } catch (error) {
    console.error('Submit term failed:', error)
    ElMessage.error(t('common.operationFailed'))
  } finally {
    termDialogLoading.value = false
  }
}

const handleDeleteTerm = async (term: TermVO) => {
  try {
    await ElMessageBox.confirm(
      t('subject.removeTermConfirm', { name: term.name }),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
    await deleteTerm(term.id)
    ElMessage.success(t('subject.deleteTermSuccess'))
    await loadTerms()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete term:', error)
      ElMessage.error(t('common.operationFailed'))
    }
  }
}

const loadTablesForRelation = async (append: boolean) => {
  if (tableLoadingMore.value) return
  tableLoadingMore.value = true
  try {
    const pageToLoad = append ? tablePage.value + 1 : 1
    const res = await getSubjectTables(props.subjectId, pageToLoad, 50, tableSearchKeyword.value.trim() || undefined)
    if (append) {
      availableTables.value = [...availableTables.value, ...(res.data ?? [])]
    } else {
      availableTables.value = res.data ?? []
    }
    tablePage.value = pageToLoad
    tableTotal.value = res.total ?? 0
  } catch (error) {
    console.error('Failed to load subject tables:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    tableLoadingMore.value = false
  }
}

const handleTableSearch = async () => {
  if (relationDialogLoading.value || tableLoadingMore.value) return
  selectedTableIds.value = new Set()
  tableLevelTableIds.value = new Set()
  selectedFieldsByTable.value = new Map()
  fieldSearchByTable.value = new Map()
  activeFieldConfigTableId.value = ''
  tableRef.value?.clearSelection()
  relationDialogLoading.value = true
  tablePage.value = 1
  availableTables.value = []
  try {
    await loadTablesForRelation(false)
  } finally {
    relationDialogLoading.value = false
  }
}

const handleTableListScroll = (event: Event) => {
  const target = event.target as HTMLElement
  const { scrollTop, scrollHeight, clientHeight } = target
  if (scrollHeight - scrollTop - clientHeight < 150 && tableHasMore.value && !tableLoadingMore.value) {
    loadTablesForRelation(true)
  }
}

const handleOpenAddRelationDialog = async (termId: string) => {
  relationTermId.value = termId
  resetRelationEditorState()

  try {
    relationDialogLoading.value = true
    tablePage.value = 1
    tableTotal.value = 0
    availableTables.value = []
    await loadTablesForRelation(false)
    if (!termDetails.value.has(termId)) {
      await loadTermDetail(termId)
    }
    relationDialogVisible.value = true
  } catch (error) {
    console.error('Failed to load subject tables:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    relationDialogLoading.value = false
  }
}

const loadColumnsForTable = async (tableId: string) => {
  if (tableColumnsData.value.has(tableId)) return
  
  const table = availableTables.value.find(t => t.id === tableId)
  if (!table || !table.datasource_id) return

  try {
    loadingTableColumns.value.add(tableId)
    const response = await listTableColumns(table.datasource_id, tableId, 1, 1000)
    tableColumnsData.value.set(tableId, response.data || [])
  } catch (error) {
    console.error('Failed to load columns:', error)
  } finally {
    loadingTableColumns.value.delete(tableId)
  }
}

const handleSubmitRelation = async () => {
  if (selectedTableIds.value.size === 0) {
    ElMessage.warning(t('subject.selectTable'))
    return
  }

  try {
    relationDialogLoading.value = true

    const existingKeys = getExistingRelationKeys(relationTermId.value)
    const payloads: LinkTermRelationRequest[] = []
    let totalIntentCount = 0

    for (const tableId of selectedTableIds.value) {
      if (tableLevelTableIds.value.has(tableId)) {
        totalIntentCount++
        const key = buildRelationKey('TABLE', tableId)
        if (!existingKeys.has(key)) {
          payloads.push({
            entity_type: 'TABLE',
            table_id: tableId
          })
        }
      }

      const fields = selectedFieldsByTable.value.get(tableId) || new Set()
      for (const fieldName of fields) {
        totalIntentCount++
        const key = buildRelationKey('FIELD', tableId, fieldName)
        if (!existingKeys.has(key)) {
          payloads.push({
            entity_type: 'FIELD',
            table_id: tableId,
            field_name: fieldName
          })
        }
      }
    }

    const skippedCount = totalIntentCount - payloads.length

    if (payloads.length === 0) {
      ElMessage.warning(t('subject.relationAlreadyExists'))
      relationDialogLoading.value = false
      return
    }

    await Promise.all(payloads.map(body => linkTermRelation(relationTermId.value, body)))

    ElMessage.success(t('subject.addRelationResult', { added: payloads.length, skipped: skippedCount }))

    await loadTermDetail(relationTermId.value)
    resetRelationEditorState()
    relationDialogVisible.value = false
  } catch (error) {
    console.error('Failed to add relations:', error)
    ElMessage.error(t('common.operationFailed'))
  } finally {
    relationDialogLoading.value = false
  }
}

const handleRemoveRelation = async (termId: string, relation: TermRelationVO) => {
  try {
    await ElMessageBox.confirm(
      t('subject.removeRelationConfirm'),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
    await unlinkTermRelation(termId, relation.table_id, relation.field_name || null)
    ElMessage.success(t('subject.removeRelationSuccess'))
    await loadTermDetail(termId)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to remove relation:', error)
      ElMessage.error(t('common.operationFailed'))
    }
  }
}

onMounted(() => {
  loadTermsAndDetails()
})
</script>

<template>
  <div class="space-y-4 py-4">
    <div class="flex items-center justify-between mb-4 gap-2 flex-wrap">
      <div class="flex items-center gap-2">
        <el-input
          v-model="searchKeyword"
          :placeholder="t('common.search')"
          clearable
          class="!w-60"
          @keyup.enter="handleSearch"
          @clear="handleClearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" plain :icon="Search" @click="handleSearch">
          {{ t('common.search') }}
        </el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleOpenTermDialog()">
        {{ t('subject.addTerm') }}
      </el-button>
    </div>

    <DataTableShell
      :loading="loading"
      :total="total"
      :page="page"
      :page-size="pageSize"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    >
      <el-table :data="termList" stripe>
        <el-table-column prop="name" :label="t('common.name')" min-width="150" />
        <el-table-column :label="t('common.aliases')" min-width="180">
          <template #default="{ row }">
            <template v-if="row.aliases && row.aliases.length > 0">
              <el-tag v-for="alias in row.aliases" :key="alias" size="small" class="mr-1">
                {{ alias }}
              </el-tag>
            </template>
            <span v-else class="text-sm text-[var(--ep-text-color-placeholder)]">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="t('common.description')" min-width="220" />
        <el-table-column :label="t('subject.linkedEntities')" min-width="260">
          <template #default="{ row }">
            <template v-if="getTermRelations(row.id).length > 0">
              <div class="flex flex-wrap gap-1 items-center">
                <el-tag
                  v-for="rel in getTermDisplayRelations(row.id)"
                  :key="rel.id"
                  :type="rel.entity_type === 'TABLE' ? 'info' : 'warning'"
                  size="small"
                  closable
                  @close="handleRemoveRelation(row.id, rel)"
                >
                  {{ getRelationDisplayText(rel) }}
                </el-tag>

                <template v-if="getTermRelations(row.id).length > 2">
                  <el-popover trigger="click" placement="bottom" :width="340">
                    <template #reference>
                      <el-button link type="primary" size="small">
                        +{{ getTermRelations(row.id).length - 2 }}
                      </el-button>
                    </template>
                    <div class="space-y-1 max-h-60 overflow-y-auto">
                      <div v-for="rel in getTermRelations(row.id)" :key="rel.id" class="flex items-center justify-between gap-2 py-1">
                        <div class="flex items-center gap-1.5 min-w-0">
                          <el-tag :type="rel.entity_type === 'TABLE' ? 'info' : 'warning'" size="small">
                            {{ rel.entity_type === 'TABLE' ? t('subject.tableLevel') : t('subject.fieldLevel') }}
                          </el-tag>
                          <span class="text-xs truncate" :title="getRelationDisplayText(rel)">{{ getRelationDisplayText(rel) }}</span>
                        </div>
                        <el-button link type="danger" size="small" @click="handleRemoveRelation(row.id, rel)">
                          {{ t('common.remove') }}
                        </el-button>
                      </div>
                    </div>
                  </el-popover>
                </template>
              </div>
            </template>
            <span v-else class="text-sm text-[var(--ep-text-color-placeholder)]">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenAddRelationDialog(row.id)">
              {{ t('subject.addRelation') }}
            </el-button>
            <el-button link type="primary" @click="handleOpenTermDialog(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDeleteTerm(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && termList.length === 0" :description="t('subject.noTerms')" />
    </DataTableShell>

    <el-dialog
      v-model="termDialogVisible"
      :title="termDialogTitle"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="termFormRef" :model="termFormData" :rules="rules" label-width="120px" @submit.prevent>
        <el-form-item :label="t('common.name')" prop="name">
          <el-input v-model="termFormData.name" :placeholder="t('common.placeholder.name')" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('common.aliases')">
          <div class="flex gap-2 flex-wrap">
            <el-tag
              v-for="alias in termFormData.aliases"
              :key="alias"
              closable
              :disable-transitions="false"
              @close="removeTermAlias(alias)"
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
          <el-input v-model="termFormData.description" :placeholder="t('common.placeholder.description')" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="handleCloseTermDialog">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="termDialogLoading" @click="handleSubmitTerm">
            {{ editingTerm ? t('common.update') : t('common.create') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="relationDialogVisible"
      :title="t('subject.addRelation')"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="relationDialogLoading" class="space-y-4">
        <div class="space-y-4">
          <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <el-card shadow="never" class="border border-[var(--ep-border-color-lighter)] [&_.el-card__body]:!p-0">
              <template #header>
                <div class="flex items-center gap-2">
                  <div class="w-full">
                    <el-input
                      v-model="tableSearchKeyword"
                      :placeholder="t('subject.searchTable')"
                      :prefix-icon="Search"
                      clearable
                      size="default"
                      @keyup.enter="handleTableSearch"
                      @clear="handleTableSearch"
                    />
                  </div>
                </div>
              </template>
              <div class="max-h-[240px] overflow-y-auto" @scroll.passive="handleTableListScroll">
                <el-table
                  ref="tableRef"
                  :data="availableTables"
                  row-key="id"
                  class="!border-0"
                  :header-cell-style="{ background: 'transparent', color: 'var(--ep-text-color-secondary)', borderBottom: '1px solid var(--ep-border-color-lighter)' }"
                  @selection-change="handleSelectionChange"
                  @row-click="handleRowClick"
                >
                  <el-table-column type="selection" width="42" />
                  <el-table-column prop="name" :label="t('common.name')" min-width="140" />
                  <el-table-column :label="t('common.schema')" width="100">
                    <template #default="{ row }">
                      <el-tag v-if="row.schema" size="small" effect="plain">{{ row.schema }}</el-tag>
                      <span v-else class="text-[var(--ep-text-color-placeholder)]">-</span>
                    </template>
                  </el-table-column>
                </el-table>
                <div v-if="!relationDialogLoading && !tableLoadingMore && availableTables.length === 0" class="py-6 text-center text-sm text-[var(--ep-text-color-placeholder)]">
                  {{ tableSearchKeyword ? t('subject.noSearchResults') : t('subject.noTables') }}
                </div>
                <div v-if="tableLoadingMore" class="py-3 text-center text-sm text-[var(--ep-text-color-placeholder)]">{{ t('common.loading') }}...</div>
              </div>
            </el-card>

            <el-card shadow="never" class="border border-[var(--ep-border-color-lighter)]">
              <template #header>
                <div class="flex items-center justify-between">
                  <span class="text-sm font-semibold text-[var(--ep-text-color-primary)]">{{ t('subject.fieldConfig') }}</span>
                  <el-tag type="info" size="small">{{ selectedTables.length }} {{ t('common.selectedItems') }}</el-tag>
                </div>
              </template>
              <div v-if="selectedTables.length === 0" class="py-12">
                <el-empty :description="t('subject.selectTable')" :image-size="56" />
              </div>
              <div v-else class="space-y-3">
                <el-tabs
                  v-model="activeFieldConfigTableId"
                  class="field-config-tabs"
                  type="card"
                  @tab-change="handleFieldConfigTabChange"
                >
                  <el-tab-pane
                    v-for="table in selectedTables"
                    :key="table.id"
                    :name="table.id"
                  >
                    <template #label>
                      <div class="flex items-center gap-2">
                        <span class="max-w-36 truncate">{{ table.name }}</span>
                        <el-tag size="small" type="warning">{{ getSelectedFieldList(table.id).length }}</el-tag>
                      </div>
                    </template>

                    <div class="space-y-3 pb-1">
                      <div class="rounded-md border border-[var(--ep-border-color-lighter)] bg-[var(--ep-fill-color-lighter)] px-3 py-2">
                        <el-switch
                          :model-value="isTableLevelEnabled(table.id)"
                          :active-text="t('subject.tableLevelRelation')"
                          inline-prompt
                          @change="(value: unknown) => handleToggleTableLevel(table.id, !!value)"
                        />
                      </div>

                      <el-input
                        :model-value="getFieldSearch(table.id)"
                        :placeholder="t('subject.searchField')"
                        :prefix-icon="Search"
                        clearable
                        @update:model-value="(value: unknown) => setFieldSearch(table.id, String(value ?? ''))"
                      />

                      <div v-loading="isTableColumnsLoading(table.id)" class="max-h-52 overflow-y-auto rounded-md border border-[var(--ep-border-color-lighter)] p-3">
                        <el-checkbox-group
                          :model-value="getSelectedFieldList(table.id)"
                          class="grid grid-cols-1 gap-2 md:grid-cols-2"
                          @change="(value) => handleFieldsChange(table.id, value as string[])"
                        >
                          <el-checkbox
                            v-for="column in getFilteredColumnsByTable(table.id)"
                            :key="column.name"
                            :value="column.name"
                            border
                            class="!mr-0 rounded-md border-[var(--ep-border-color-lighter)] bg-white px-3 py-2"
                          >
                            {{ column.name }}
                          </el-checkbox>
                        </el-checkbox-group>
                        <div v-if="getFilteredColumnsByTable(table.id).length === 0" class="py-6 text-center text-sm text-[var(--ep-text-color-placeholder)]">
                          {{ getFieldEmptyTextByTable(table.id) }}
                        </div>
                      </div>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-card>
          </div>

        </div>
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="relationDialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button
            type="primary"
            :disabled="selectedTableIds.size === 0 || selectedRelationCount === 0"
            :loading="relationDialogLoading"
            @click="handleSubmitRelation"
          >
            {{ t('common.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
