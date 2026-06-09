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
const relationMode = ref<'list' | 'add'>('list')

const availableTables = ref<TableInfoVO[]>([])
const selectedTableIds = ref<Set<string>>(new Set())
const tableLevelTableIds = ref<Set<string>>(new Set())
const selectedFieldsByTable = ref<Map<string, Set<string>>>(new Map())
const fieldSearchByTable = ref<Map<string, string>>(new Map())
const activeFieldConfigTableId = ref('')
const tableColumnsData = ref<Map<string, { name: string }[]>>(new Map())
const loadingTableColumns = ref<Set<string>>(new Set())
const tableSearch = ref('')
const schemaFilter = ref('')

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
    termList.value = await getTermsBySubject(props.subjectId)
  } catch (error) {
    console.error('Failed to load terms:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    loading.value = false
  }
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

const schemaOptions = computed(() => {
  const schemaSet = new Set<string>()
  for (const table of availableTables.value) {
    if (table.schema) schemaSet.add(table.schema)
  }
  return Array.from(schemaSet).sort((a, b) => a.localeCompare(b))
})

const filteredTables = computed(() => {
  let list = availableTables.value
  if (schemaFilter.value) {
    list = list.filter(table => table.schema === schemaFilter.value)
  }
  if (!tableSearch.value) return list
  const search = tableSearch.value.toLowerCase()
  return list.filter(table => table.name.toLowerCase().includes(search))
})

const selectedTables = computed(() => {
  return availableTables.value.filter(table => selectedTableIds.value.has(table.id))
})

const tableEmptyText = computed(() => {
  if (availableTables.value.length === 0) return t('subject.noTables')
  if (tableSearch.value && filteredTables.value.length === 0) return t('subject.noSearchResults')
  return ''
})

const selectedRelationCount = computed(() => {
  let count = tableLevelTableIds.value.size
  for (const fields of selectedFieldsByTable.value.values()) {
    count += fields.size
  }
  return count
})

const isTableSelected = (tableId: string) => selectedTableIds.value.has(tableId)

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
  tableSearch.value = ''
  schemaFilter.value = ''
}

const handleTableToggle = async (tableId: string, checked: boolean) => {
  if (checked) {
    selectedTableIds.value.add(tableId)
    tableLevelTableIds.value.add(tableId)
    if (!selectedFieldsByTable.value.has(tableId)) {
      selectedFieldsByTable.value.set(tableId, new Set())
    }
    if (!activeFieldConfigTableId.value) {
      activeFieldConfigTableId.value = tableId
    }
    await loadColumnsForTable(tableId)
  } else {
    selectedTableIds.value.delete(tableId)
    tableLevelTableIds.value.delete(tableId)
    selectedFieldsByTable.value.delete(tableId)
    fieldSearchByTable.value.delete(tableId)
    if (activeFieldConfigTableId.value === tableId) {
      activeFieldConfigTableId.value = selectedTables.value[0]?.id || ''
    }
  }
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

const handleOpenRelationDialog = async (termId: string) => {
  relationTermId.value = termId
  relationMode.value = 'list'
  resetRelationEditorState()

  try {
    relationDialogLoading.value = true
    availableTables.value = await getSubjectTables(props.subjectId)
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

const handleCloseRelationDialog = () => {
  relationDialogVisible.value = false
  relationMode.value = 'list'
  resetRelationEditorState()
}

const handleAddRelation = () => {
  relationMode.value = 'add'
  resetRelationEditorState()
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
    relationMode.value = 'list'
    resetRelationEditorState()
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
    <div class="flex items-center justify-end">
      <el-button type="primary" :icon="Plus" @click="handleOpenTermDialog()">
        {{ t('subject.addTerm') }}
      </el-button>
    </div>

    <el-table :data="termList" v-loading="loading" stripe border>
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
            <el-space wrap :size="6">
              <el-tooltip
                v-for="relation in getTermDisplayRelations(row.id)"
                :key="relation.id"
                :content="getRelationDisplayText(relation)"
                placement="top"
              >
                <el-tag :type="relation.entity_type === 'TABLE' ? 'info' : 'warning'" size="small">
                  {{ getRelationDisplayText(relation) }}
                </el-tag>
              </el-tooltip>
              <span v-if="getTermRelations(row.id).length > 2" class="text-xs text-[var(--ep-text-color-placeholder)]">
                +{{ getTermRelations(row.id).length - 2 }}
              </span>
            </el-space>
          </template>
          <span v-else class="text-sm text-[var(--ep-text-color-placeholder)]">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleOpenRelationDialog(row.id)">
            {{ t('subject.manageRelation') }}
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
      :title="relationMode === 'list' ? t('subject.manageRelation') : t('subject.addRelation')"
      width="780px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="relationDialogLoading" class="space-y-4">
        <div v-if="relationMode === 'list'" class="space-y-4">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium text-[var(--ep-text-color-primary)]">
              {{ t('subject.existingRelations') }} ({{ getTermRelations(relationTermId).length }})
            </div>
            <el-button type="primary" size="small" @click="handleAddRelation">
              {{ t('subject.addRelation') }}
            </el-button>
          </div>
          <el-table v-if="getTermRelations(relationTermId).length > 0" :data="getTermRelations(relationTermId)" border>
            <el-table-column :label="t('subject.selectType')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.entity_type === 'TABLE' ? 'info' : 'warning'" size="small">
                  {{ row.entity_type === 'TABLE' ? t('subject.tableLevel') : t('subject.fieldLevel') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('subject.selectTarget')" min-width="380">
              <template #default="{ row }">
                <span class="block truncate" :title="getRelationDisplayText(row)">{{ getRelationDisplayText(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('common.actions')" width="120">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="handleRemoveRelation(relationTermId, row)">
                  {{ t('common.remove') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :description="t('subject.noRelations')" :image-size="60" />
        </div>

        <div v-else class="space-y-4">
          <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <el-card shadow="never" class="border border-[var(--ep-border-color-lighter)]">
              <template #header>
                <div class="flex items-center gap-2">
                  <span class="whitespace-nowrap text-sm font-semibold text-[var(--ep-text-color-primary)]">{{ t('subject.selectTableStep') }}</span>
                  <div class="w-64">
                    <el-select
                      v-model="schemaFilter"
                      clearable
                      filterable
                      :placeholder="t('subject.filterSchema')"
                      size="default"
                      class="w-full"
                      popper-class="schema-filter-popper"
                    >
                      <el-option v-for="schema in schemaOptions" :key="schema" :label="schema" :value="schema">
                        <span class="block truncate" :title="schema">{{ schema }}</span>
                      </el-option>
                    </el-select>
                  </div>
                  <div class="ml-auto w-full max-w-xs">
                    <el-input
                      v-model="tableSearch"
                      :placeholder="t('subject.searchTable')"
                      :prefix-icon="Search"
                      clearable
                      size="default"
                    />
                  </div>
                </div>
              </template>
              <div class="max-h-[380px] space-y-2 overflow-y-auto pr-1">
                <el-card
                  v-for="table in filteredTables"
                  :key="table.id"
                  shadow="never"
                  class="border border-[var(--ep-border-color-lighter)]"
                >
                  <div class="flex items-center justify-between gap-3">
                    <el-checkbox :model-value="isTableSelected(table.id)" @change="(value: unknown) => handleTableToggle(table.id, !!value)">
                      <span class="text-sm font-medium text-[var(--ep-text-color-primary)]">{{ table.name }}</span>
                    </el-checkbox>
                    <div class="flex items-center gap-2">
                      <el-tag v-if="table.schema" size="small" effect="plain">{{ table.schema }}</el-tag>
                      <el-tag v-if="isTableSelected(table.id)" type="primary" size="small">{{ t('common.selected') }}</el-tag>
                    </div>
                  </div>
                </el-card>
                <div v-if="tableEmptyText" class="py-8 text-center text-sm text-[var(--ep-text-color-placeholder)]">{{ tableEmptyText }}</div>
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
          <template v-if="relationMode === 'add'">
            <el-button @click="relationMode = 'list'">{{ t('common.cancel') }}</el-button>
            <el-button
              type="primary"
              :disabled="selectedTableIds.size === 0 || selectedRelationCount === 0"
              :loading="relationDialogLoading"
              @click="handleSubmitRelation"
            >
              {{ t('common.confirm') }}
            </el-button>
          </template>
          <template v-else>
            <el-button @click="handleCloseRelationDialog">{{ t('common.close') }}</el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
