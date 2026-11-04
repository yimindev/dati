<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { DatasourceVO, DataSourcePayload } from '~/api/datasource'
import { addDataSource, updateDataSource, testConnection } from '~/api/datasource'
import { normalizeError, resolveErrorMessage } from '~/api/error'
import { toDatasourcePayload } from './payload'

// i18n
const { t } = useI18n()

// Props & Emits
interface Props {
  modelValue: boolean
  datasource?: DatasourceVO | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const formRef = ref()
const submitting = ref(false)
const testing = ref(false)
/** 最近一次测试连接/保存失败的原因，常驻在弹窗内，避免 toast 一闪而过。 */
const errorMessage = ref('')

// 表单数据
const formData = ref<DataSourcePayload>({
  name: '',
  description: '',
  jdbc_url: '',
  username: '',
  password: '',
  type: 'MYSQL',
})

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.datasource)

const showError = (error: unknown, fallback: string) => {
  errorMessage.value = resolveErrorMessage(normalizeError(error), fallback)
}

// 测试连接：编辑态带上 id，密码留空时由后端使用已保存的密码
const connectionPayload = (): DataSourcePayload => ({
  ...formData.value,
  ...(isEdit.value && props.datasource ? { id: props.datasource.id } : {}),
})

// 监听 datasource 变化，重置表单
watch(() => props.datasource, (newVal) => {
  if (newVal) {
    formData.value = {
      name: newVal.name,
      description: newVal.description || '',
      jdbc_url: newVal.jdbc_url,
      username: newVal.username,
      password: '',
      type: newVal.type,
    }
  } else {
    resetForm()
  }
  errorMessage.value = ''
}, { immediate: true })

watch(visible, (open) => {
  if (open) {
    errorMessage.value = ''
  }
})

// 重置表单
function resetForm() {
  formData.value = {
    name: '',
    description: '',
    jdbc_url: '',
    username: '',
    password: '',
    type: 'MYSQL',
  }
  formRef.value?.resetValidation()
  errorMessage.value = ''
}

// 连接相关字段变更后，旧的失败原因已失效
watch(
  () => ({ url: formData.value.jdbc_url, user: formData.value.username, pass: formData.value.password, type: formData.value.type }),
  () => { errorMessage.value = '' }
)

// 测试连接
const handleTestConnection = async () => {
  const valid = await formRef.value?.validateConnectionFields?.()
  if (!valid) return

  try {
    testing.value = true
    errorMessage.value = ''
    const result = await testConnection(connectionPayload())

    if (result) {
      ElMessage.success(t('datasource.testSuccess'))
    }
  } catch (error) {
    console.error('Failed to test connection:', error)
    showError(error, t('datasource.testFailed'))
  } finally {
    testing.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value?.validate?.()
  if (!valid) return

  try {
    submitting.value = true
    errorMessage.value = ''
    const payload = toDatasourcePayload(formData.value, isEdit.value)

    if (isEdit.value) {
      await updateDataSource(props.datasource!.id, payload)
    } else {
      await addDataSource(payload)
    }

    ElMessage.success(t('common.saveSuccess'))
    emit('success')
  } catch (error) {
    console.error('Failed to submit:', error)
    showError(error, t('common.operationFailed'))
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? t('datasource.editTitle') : t('datasource.createTitle')"
    width="35%"
    :close-on-click-modal="false"
  >
    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <DatasourceForm
      ref="formRef"
      v-model="formData"
      :is-edit="isEdit"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="testing" @click="handleTestConnection">
          {{ t('common.testConnection') }}
        </el-button>
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
