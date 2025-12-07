<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
  (e: 'table-manage', datasource: DatasourceVO): void
}

defineProps<Props>()
defineEmits<Emits>()

const { locale } = useI18n()

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    return new Intl.DateTimeFormat(locale.value).format(new Date(dateStr))
  } catch {
    return new Date(dateStr).toLocaleString()
  }
}
</script>

<template>
    <el-table
      :data="data"
      :loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="id" :label="$t('datasource.table.columns.id')" min-width="150" />
      <el-table-column prop="name" :label="$t('datasource.table.columns.name')" min-width="100" />
      <el-table-column prop="type" :label="$t('datasource.table.columns.type')" min-width="120" />
      <el-table-column prop="created_by" :label="$t('datasource.table.columns.createdBy')" width="120" />
      <el-table-column prop="description" :label="$t('datasource.table.columns.description')" min-width="150" show-overflow-tooltip />
      <el-table-column prop="updated_at" :label="$t('datasource.table.columns.updatedAt')" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('datasource.table.columns.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button type="primary" link @click="$emit('table-manage', row)">{{ $t('datasource.table.actions.tableManage') }}</el-button>
            <el-button type="primary" link @click="$emit('edit', row)">{{ $t('datasource.table.actions.edit') }}</el-button>
            <el-button type="danger" link @click="$emit('delete', row)">{{ $t('datasource.table.actions.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
</template>
