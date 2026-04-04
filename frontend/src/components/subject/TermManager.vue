<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import type { TermVO, TermRelationVO, CreateTermRequest, UpdateTermRequest, LinkTermRelationRequest, SubjectTableVO } from '~/api/subject'
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
const expandedRows = ref<string[]>([])

const termDialogVisible = ref(false)
const termDialogLoading = ref(false)
const editingTerm = ref<TermVO | null>(null)
const termFormRef = ref<FormInstance>()
const termFormData = ref<CreateTermRequest>({ name: '', description: '' })

const relationDialogVisible = ref(false)
const relationDialogLoading = ref(false)
const relationTermId = ref('')
const availableTables = ref<SubjectTableVO[]>([])
const selectedTableId = ref('')
const tableColumns = ref<{ name: string }[]>([])
const columnsLoading = ref(false)
const selectedField = ref('')

const rules: FormRules = {
  name: [
    { required: true, message: t('common.required', { name: t('common.name') }), trigger: 'blur' },
    { min: 1, max: 100, message: t('common.nameLengthError'), trigger: 'blur' }
  ]
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

const handleRowExpand = async (row: TermVO) => {
  const isExpanded = expandedRows.value.includes(row.id)
  if (isExpanded) {
    expandedRows.value = expandedRows.value.filter(id => id !== row.id)
  } else {
    expandedRows.value.push(row.id)
    if (!termDetails.value.has(row.id)) {
      await loadTermDetail(row.id)
    }
  }
}

const getTermRelations = (termId: string) => {
  return termDetails.value.get(termId)?.relations || []
}

const handleOpenTermDialog = (term?: TermVO) => {
  if (term) {
    editingTerm.value = term
    termFormData.value = {
      name: term.name,
      description: term.description || ''
    }
  } else {
    editingTerm.value = null
    termFormData.value = { name: '', description: '' }
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
        description: termFormData.value.description
      }
      await updateTerm(editingTerm.value.id, updateData)
      ElMessage.success(t('subject.updateTermSuccess'))
    } else {
      await createTerm(props.subjectId, termFormData.value)
      ElMessage.success(t('subject.addTermSuccess'))
    }

    handleCloseTermDialog()
    await loadTerms()
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
  selectedTableId.value = ''
  selectedField.value = ''
  tableColumns.value = []

  try {
    relationDialogLoading.value = true
    availableTables.value = await getSubjectTables(props.subjectId)
    relationDialogVisible.value = true
  } catch (error) {
    console.error('Failed to load subject tables:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    relationDialogLoading.value = false
  }
}

const handleTableChange = async (tableId: string) => {
  selectedField.value = ''
  if (!tableId) {
    tableColumns.value = []
    return
  }

  const table = availableTables.value.find(t => t.table_id === tableId)
  if (!table) return

  try {
    columnsLoading.value = true
    const tableInfo = availableTables.value.find(t => t.table_id === tableId)
    if (tableInfo) {
      const response = await listTableColumns(tableInfo.table_id, tableId, 1, 1000)
      tableColumns.value = response.data || []
    }
  } catch (error) {
    console.error('Failed to load columns:', error)
    tableColumns.value = []
  } finally {
    columnsLoading.value = false
  }
}

const handleSubmitRelation = async () => {
  if (!selectedTableId.value) {
    ElMessage.warning(t('subject.selectTable'))
    return
  }

  try {
    relationDialogLoading.value = true
    const body: LinkTermRelationRequest = {
      entity_type: selectedField.value ? 'FIELD' : 'TABLE',
      table_id: selectedTableId.value,
      field_name: selectedField.value || undefined
    }
    await linkTermRelation(relationTermId.value, body)
    ElMessage.success(t('subject.addRelationSuccess'))

    relationDialogVisible.value = false
    await loadTermDetail(relationTermId.value)
  } catch (error) {
    console.error('Failed to add relation:', error)
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
    await unlinkTermRelation(termId, relation.table_id, relation.field_name || '')
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
  loadTerms()
})
</script>

<template>
  <div class="term-manager">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-medium">{{ t('subject.termManagement') }}</h3>
      <el-button type="primary" :icon="Plus" @click="handleOpenTermDialog()">
        {{ t('subject.addTerm') }}
      </el-button>
    </div>

    <el-table
      :data="termList"
      v-loading="loading"
      stripe
      row-key="id"
      :expand-row-keys="expandedRows"
      @expand-change="handleRowExpand"
      type="expand"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="term-detail p-4">
            <p class="term-description mb-4" v-if="row.description">{{ row.description }}</p>
            <p class="term-description mb-4 text-gray-400" v-else>{{ t('common.placeholder.description') }}</p>

            <div class="relations-section">
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm text-gray-600">{{ t('subject.linkedTables') }}</span>
                <el-button size="small" @click="handleOpenRelationDialog(row.id)">
                  {{ t('subject.addRelation') }}
                </el-button>
              </div>

              <div v-if="getTermRelations(row.id).length > 0" class="relation-list">
                <div
                  v-for="relation in getTermRelations(row.id)"
                  :key="relation.id"
                  class="relation-item flex items-center justify-between py-2"
                >
                  <div class="flex items-center">
                    <el-tag v-if="relation.entity_type === 'TABLE'" size="small" class="mr-2">
                      {{ t('subject.tableLevel') }}
                    </el-tag>
                    <el-tag v-else size="small" type="info" class="mr-2">
                      {{ t('subject.fieldLevel') }}
                    </el-tag>
                    <span v-if="relation.entity_type === 'TABLE'">
                      {{ t('subject.linkToTable') }}: {{ relation.table_name }} ({{ t('subject.tableLevel') }})
                    </span>
                    <span v-else>
                      {{ t('subject.linkToField') }}: {{ relation.table_name }}.{{ relation.field_name }}
                    </span>
                  </div>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click="handleRemoveRelation(row.id, relation)"
                  >
                    {{ t('common.remove') }}
                  </el-button>
                </div>
              </div>
              <el-empty v-else :description="t('subject.noTerms')" :image-size="60" />
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="name" :label="t('common.name')" min-width="150" />
      <el-table-column prop="description" :label="t('common.description')" min-width="200" />
      <el-table-column :label="t('subject.linkedTables')" width="100">
        <template #default="{ row }">
          {{ termDetails.get(row.id)?.relations?.length || 0 }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="150" fixed="right">
        <template #default="{ row }">
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
      :title="editingTerm ? t('subject.editTerm') : t('subject.addTerm')"
      width="35%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="termFormRef"
        :model="termFormData"
        :rules="rules"
        label-width="100px"
        @submit.prevent
      >
        <el-form-item :label="t('common.name')" prop="name">
          <el-input
            v-model="termFormData.name"
            :placeholder="t('common.placeholder.name')"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item :label="t('common.description')">
          <el-input
            v-model="termFormData.description"
            :placeholder="t('common.placeholder.description')"
            type="textarea"
            :rows="3"
            maxlength="500"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCloseTermDialog">{{ t('common.cancel') }}</el-button>
          <el-button
            type="primary"
            :loading="termDialogLoading"
            @click="handleSubmitTerm"
          >
            {{ editingTerm ? t('common.update') : t('common.create') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="relationDialogVisible"
      :title="t('subject.addRelation')"
      width="35%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item :label="t('common.tableName')" required>
          <el-select
            v-model="selectedTableId"
            :placeholder="t('subject.selectTable')"
            :loading="relationDialogLoading"
            style="width: 100%"
            @change="handleTableChange"
          >
            <el-option
              v-for="table in availableTables"
              :key="table.table_id"
              :label="table.table_name"
              :value="table.table_id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('column.columnName')">
          <el-select
            v-model="selectedField"
            :placeholder="t('subject.selectField')"
            :loading="columnsLoading"
            :disabled="!selectedTableId"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="column in tableColumns"
              :key="column.name"
              :label="column.name"
              :value="column.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <span class="text-sm text-gray-500">{{ t('subject.fieldOptional') }}</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="relationDialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button
            type="primary"
            :loading="relationDialogLoading"
            :disabled="!selectedTableId"
            @click="handleSubmitRelation"
          >
            {{ t('common.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.term-manager {
  padding: 16px 0;
}

.term-description {
  color: #666;
  font-size: 14px;
}

.relations-section {
  padding: 8px 0;
}

.relation-item {
  border-bottom: 1px solid #f0f0f0;
}

.relation-item:last-child {
  border-bottom: none;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
