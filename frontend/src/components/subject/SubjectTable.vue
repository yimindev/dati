<script setup lang="ts">
import type { SubjectVO } from '~/api/subject'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  data: SubjectVO[]
  loading?: boolean
}

interface Emits {
  (e: 'detail', subject: SubjectVO): void
  (e: 'edit', subject: SubjectVO): void
  (e: 'delete', subject: SubjectVO): void
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
    @row-click="(row: SubjectVO) => $emit('detail', row)"
  >
    <el-table-column prop="name" :label="t('common.name')" min-width="160">
      <template #default="{ row }">
        <el-button link type="primary" @click.stop="$emit('detail', row)">
          {{ row.name }}
        </el-button>
      </template>
    </el-table-column>
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
    <el-table-column :label="t('subject.datasource')" min-width="140">
      <template #default="{ row }">
        {{ row.datasource_name || row.datasource_id }}
      </template>
    </el-table-column>
    <el-table-column prop="description" :label="t('common.description')" min-width="200" show-overflow-tooltip />
    <el-table-column :label="t('common.actions')" width="150" fixed="right">
      <template #default="{ row }">
        <div class="flex items-center gap-2">
          <el-button type="primary" link @click.stop="$emit('edit', row)">
            {{ t('common.edit') }}
          </el-button>
          <el-button type="danger" link @click.stop="$emit('delete', row)">
            {{ t('common.delete') }}
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>
