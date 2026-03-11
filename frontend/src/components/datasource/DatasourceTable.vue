<script setup lang="ts">
import type { DatasourceVO } from '~/api/datasource'
import { formatDateTime } from "~/composables";
import {useI18n} from "vue-i18n";

const { t } = useI18n();

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
</script>

<template>
    <el-table
      :data="data"
      :loading="loading"
      stripe
      style="width: 100%"
    >
      <el-table-column prop="id" :label="t('common.id')" min-width="150" />
      <el-table-column prop="name" :label="t('common.name')" min-width="100" />
      <el-table-column prop="type" :label="t('common.type')" min-width="100" />
      <el-table-column prop="created_by" :label="t('common.createdBy')" min-width="100" />
      <el-table-column prop="description" :label="t('common.description')" min-width="120" show-overflow-tooltip />
      <el-table-column prop="updated_at" :label="t('common.updatedAt')" min-width="100">
        <template #default="{ row }">
          {{ formatDateTime(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="220" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button type="primary" link @click="$emit('table-manage', row)">{{ t('datasource.tableManage') }}</el-button>
            <el-button type="primary" link @click="$emit('edit', row)">{{ t('common.edit') }}</el-button>
            <el-button type="danger" link @click="$emit('delete', row)">{{ t('common.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
</template>
