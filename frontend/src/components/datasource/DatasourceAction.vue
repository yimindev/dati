<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { DatasourceVO, DataSourcePayload } from '~/api/datasource'
import { addDataSource, updateDataSource, testConnection } from '~/api/datasource'

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

// 表单数据
const formData = ref<DataSourcePayload>({
  name: '',
  description: '',
  jdbc_url: '',
  username: '',
  password: '',
  type: 'MySQL'
})

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.datasource)

// 监听 datasource 变化，重置表单
watch(() => props.datasource, (newVal) => {
  if (newVal) {
    formData.value = {
      name: newVal.name,
      description: newVal.description || '',
      jdbc_url: newVal.jdbc_url,
      username: newVal.username,
      password: '',
      type: newVal.type
    }
  } else {
    resetForm()
  }
}, { immediate: true })

// 重置表单
const resetForm = () => {
  formData.value = {
    name: '',
    description: '',
    jdbc_url: '',
    username: '',
    password: '',
    type: 'MySQL'
  }
  formRef.value?.resetValidation()
}

// 测试连接
const handleTestConnection = async () => {
  try {
    if (!formRef.value?.validate()) return

    testing.value = true
    const result = await testConnection(formData.value)

    if (result) {
      ElMessage.success(t('datasource.messages.testSuccess'))
    } else {
      ElMessage.error(t('datasource.messages.testFailed'))
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error(t('datasource.messages.testFailed'))
  } finally {
    testing.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    if (!formRef.value?.validate()) return

    submitting.value = true

    if (isEdit.value) {
      await updateDataSource(props.datasource!.id, formData.value)
      ElMessage.success(t('datasource.messages.updateSuccess'))
    } else {
      await addDataSource(formData.value)
      ElMessage.success(t('datasource.messages.createSuccess'))
    }

    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(t('datasource.messages.operateFailed'))
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
    :title="isEdit ? $t('datasource.dialog.editTitle') : $t('datasource.dialog.createTitle')"
    width="600px"
    :close-on-click-modal="false"
  >
    <DatasourceForm
      ref="formRef"
      v-model="formData"
      :loading="submitting"
      @test-connection="handleTestConnection"
    />

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">{{ $t('datasource.common.cancel') }}</el-button>
        <el-button type="primary" :loading="testing" @click="handleTestConnection">
          {{ $t('datasource.common.testConnection') }}
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? $t('datasource.common.update') : $t('datasource.common.create') }}
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