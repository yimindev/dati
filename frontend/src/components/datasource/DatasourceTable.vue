<script setup lang="ts">
import type { DatasourceVO } from '~/api/datasource'
import { formatDateTime } from "~/composables";
import {useI18n} from "vue-i18n";

const { t } = useI18n();

interface Props {
  data: DatasourceVO[]
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
      stripe
      style="width: 100%"
    >
      <el-table-column prop="name" :label="t('common.name')" min-width="180">
        <template #default="{ row }">
          <div class="flex flex-col gap-0.5 min-w-0">
            <el-button
              link
              type="primary"
              class="!justify-start font-medium text-left !p-0 truncate"
              @click="$emit('table-manage', row)"
            >
              {{ row.name }}
            </el-button>
            <span v-if="row.id" class="text-xs text-[var(--ep-text-color-placeholder)] font-mono truncate">
              {{ row.id }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="type" :label="t('common.type')" min-width="110" />
      <el-table-column prop="created_user_name" :label="t('common.createdBy')" min-width="100" />
      <el-table-column prop="description" :label="t('common.description')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="updated_at" :label="t('common.updatedAt')" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.updated_at) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="220" fixed="right" align="right">
        <template #default="{ row }">
          <div class="flex items-center justify-end gap-2">
            <el-button type="primary" link @click="$emit('table-manage', row)">{{ t('datasource.tableManage') }}</el-button>
            <el-button type="primary" link @click="$emit('edit', row)">{{ t('common.edit') }}</el-button>
            <el-button type="danger" link @click="$emit('delete', row)">{{ t('common.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('datasource.emptyList')" />
      </template>
    </el-table>
</template>
