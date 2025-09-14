<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { DataSourcePayload } from '~/api/datasource'

// Props & Emits
interface Props {
  modelValue: DataSourcePayload
  loading?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: DataSourcePayload): void
  (e: 'test-connection'): void
}

defineProps<Props>()
defineEmits<Emits>()

// 表单引用
const formRef = ref<FormInstance>()

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入连接名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据库类型', trigger: 'change' }
  ],
  jdbc_url: [
    { required: true, message: '请输入JDBC连接字符串', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 暴露验证方法
const validate = async () => {
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

const resetValidation = () => {
  formRef.value?.clearValidate()
}

// 暴露给父组件
defineExpose({
  validate,
  resetValidation
})
</script>

<template>
  <el-form
    ref="formRef"
    :model="modelValue"
    :rules="rules"
    label-width="100px"
    @submit.prevent
  >
    <el-form-item label="连接名称" prop="name">
      <el-input
        v-model="modelValue.name"
        placeholder="请输入连接名称"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item label="数据库类型" prop="type">
      <el-select
        v-model="modelValue.type"
        placeholder="请选择数据库类型"
        style="width: 100%"
      >
        <el-option label="MySQL" value="MySQL" />
        <el-option label="PostgreSQL" value="PostgreSQL" />
        <el-option label="Oracle" value="Oracle" />
        <el-option label="SQL Server" value="SQL Server" />
      </el-select>
    </el-form-item>

    <el-form-item label="JDBC URL" prop="jdbc_url">
      <el-input
        v-model="modelValue.jdbc_url"
        placeholder="请输入JDBC连接字符串"
        :rows="2"
        type="textarea"
      />
    </el-form-item>

    <el-form-item label="用户名" prop="username">
      <el-input
        v-model="modelValue.username"
        placeholder="请输入数据库用户名"
      />
    </el-form-item>

    <el-form-item label="密码" prop="password">
      <el-input
        v-model="modelValue.password"
        type="password"
        placeholder="请输入数据库密码"
        show-password
      />
    </el-form-item>

    <el-form-item label="描述">
      <el-input
        v-model="modelValue.description"
        placeholder="请输入描述（可选）"
        :rows="3"
        type="textarea"
        maxlength="500"
      />
    </el-form-item>
  </el-form>
</template>
