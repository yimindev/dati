<script setup lang="ts">
import type { SubjectVO } from '~/api/subject'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  data: SubjectVO[]
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
    stripe
    style="width: 100%"
    @row-click="(row: SubjectVO) => $emit('detail', row)"
  >
    <el-table-column prop="name" :label="t('common.name')" min-width="180">
      <template #default="{ row }">
        <div class="flex flex-col gap-0.5 min-w-0" @click.stop>
          <el-button
            link
            type="primary"
            class="!justify-start font-medium text-left !p-0 truncate"
            @click="$emit('detail', row)"
          >
            {{ row.name }}
          </el-button>
          <span v-if="row.id" class="text-xs text-[var(--ep-text-color-placeholder)] font-mono truncate">
            {{ row.id }}
          </span>
        </div>
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
    <el-table-column :label="t('common.actions')" width="150" fixed="right" align="right">
      <template #default="{ row }">
        <div class="flex items-center justify-end gap-2">
          <el-button type="primary" link @click.stop="$emit('edit', row)">
            {{ t('common.edit') }}
          </el-button>
          <el-button type="danger" link @click.stop="$emit('delete', row)">
            {{ t('common.delete') }}
          </el-button>
        </div>
      </template>
    </el-table-column>
    <template #empty>
      <el-empty :description="t('subject.noSubject')" />
    </template>
  </el-table>
</template>
