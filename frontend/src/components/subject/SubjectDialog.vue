<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { FormInstance, FormRules } from 'element-plus'
import type { SubjectVO, CreateSubjectRequest, UpdateSubjectRequest } from '~/api/subject'
import type { DatasourceVO } from '~/api/datasource'
import { createSubject, updateSubject } from '~/api/subject'
import { listDataSources } from '~/api/datasource'

const { t } = useI18n()

interface Props {
  modelValue: boolean
  subject?: SubjectVO | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const datasources = ref<DatasourceVO[]>([])
const datasourceLoading = ref(false)

const formData = ref<CreateSubjectRequest>({
  name: '',
  description: '',
  datasource_id: '',
  aliases: []
})

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.subject)

const rules: FormRules = {
  name: [
    { required: true, message: t('common.required', { name: t('common.name') }), trigger: 'blur' },
    { min: 1, max: 100, message: t('common.nameLengthError'), trigger: 'blur' }
  ]
}

watch(() => props.subject, (newVal) => {
  if (newVal) {
    formData.value = {
      name: newVal.name,
      description: newVal.description || '',
      datasource_id: newVal.datasource_id,
      aliases: newVal.aliases ? [...newVal.aliases] : []
    }
  } else {
    resetForm()
  }
}, { immediate: true })

watch(() => props.modelValue, async (newVal) => {
  if (newVal && !isEdit.value) {
    await loadDatasources()
  }
})

async function loadDatasources() {
  try {
    datasourceLoading.value = true
    const response = await listDataSources(1, 1000)
    datasources.value = response.data || []
  } catch (error) {
    console.error('Failed to load datasources:', error)
    ElMessage.error(t('common.loadFailed'))
  } finally {
    datasourceLoading.value = false
  }
}

function resetForm() {
  formData.value = {
    name: '',
    description: '',
    datasource_id: '',
    aliases: []
  }
  formRef.value?.clearValidate()
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

const handleSubmit = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) return

    submitting.value = true

    if (isEdit.value) {
      const updateData: UpdateSubjectRequest = {
        name: formData.value.name,
        description: formData.value.description,
        aliases: formData.value.aliases
      }
      await updateSubject(props.subject!.id, updateData)
      ElMessage.success(t('subject.updateSuccess'))
    } else {
      await createSubject(formData.value)
      ElMessage.success(t('subject.createSuccess'))
    }

    emit('success')
    visible.value = false
  } catch (error) {
    console.error('Submit failed:', error)
    ElMessage.error(t('common.operationFailed'))
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? t('subject.editTitle') : t('subject.createTitle')"
    width="35%"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      @submit.prevent
    >
      <el-form-item :label="t('common.name')" prop="name">
        <el-input
          v-model="formData.name"
          :placeholder="t('common.placeholder.name')"
          maxlength="100"
          show-word-limit
        />
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
          :placeholder="t('common.placeholder.description')"
          type="textarea"
          :rows="3"
          maxlength="500"
        />
      </el-form-item>

      <el-form-item :label="t('subject.datasource')" prop="datasource_id">
        <el-select
          v-model="formData.datasource_id"
          :placeholder="t('subject.selectDatasource')"
          :disabled="isEdit"
          :loading="datasourceLoading"
          style="width: 100%"
        >
          <el-option
            v-for="ds in datasources"
            :key="ds.id"
            :label="ds.name"
            :value="ds.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ isEdit ? t('common.update') : t('common.create') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
