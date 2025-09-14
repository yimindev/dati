<script setup lang="ts">
import type { DatasourceVO } from '~/api/datasource'

// Props & Emits
interface Props {
  data: DatasourceVO[]
  loading?: boolean
}

interface Emits {
  (e: 'edit', datasource: DatasourceVO): void
  (e: 'delete', datasource: DatasourceVO): void
  (e: 'test-connection', datasource: DatasourceVO): void
}

defineProps<Props>()
defineEmits<Emits>()

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
    <el-table
      :data="data"
      :loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="id" label="ID" min-width="150" />
      <el-table-column prop="name" label="连接名称" min-width="100" />
      <el-table-column prop="type" label="类型" min-width="120">
        <template #default="{ row }">
          <DatasourceTypeTag :type="row.type" />
        </template>
      </el-table-column>
      <el-table-column prop="created_by" label="创建人" width="120" />
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column prop="updated_at" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button type="primary" link @click="$emit('edit', row)">编辑</el-button>
            <el-button type="danger" link @click="$emit('delete', row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
</template>
