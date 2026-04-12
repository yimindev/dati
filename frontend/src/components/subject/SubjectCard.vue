<script setup lang="ts">
import type { SubjectVO } from '~/api/subject'
import { formatDateTime } from '~/composables'
import { useI18n } from 'vue-i18n'
import { MoreFilled } from '@element-plus/icons-vue'

const { t } = useI18n()

interface Props {
  subject: SubjectVO
}

interface Emits {
  (e: 'edit', subject: SubjectVO): void
  (e: 'delete', subject: SubjectVO): void
  (e: 'click', subject: SubjectVO): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const handleCardClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (target?.closest('[data-stop-card-click="true"]')) {
    return
  }
  emit('click', props.subject)
}
</script>

<template>
  <el-card
    class="subject-card cursor-pointer transition-all duration-200 hover:-translate-y-0.5"
    shadow="hover"
    @click="handleCardClick"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <span class="truncate font-medium text-slate-800">{{ subject.name }}</span>
        <el-dropdown trigger="click" data-stop-card-click="true">
          <el-button text :icon="MoreFilled" data-stop-card-click="true" />
          <template #dropdown>
            <el-dropdown-menu data-stop-card-click="true">
              <el-dropdown-item data-stop-card-click="true" @click="$emit('edit', subject)">{{ t('common.edit') }}</el-dropdown-item>
              <el-dropdown-item data-stop-card-click="true" @click="$emit('delete', subject)">
                <span class="text-red-500">{{ t('common.delete') }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>

    <template #default>
      <p
        v-if="subject.description"
        class="mb-3 line-clamp-2 min-h-10 text-sm text-slate-600"
      >
        {{ subject.description }}
      </p>
      <p v-else class="mb-3 min-h-10 text-sm italic text-slate-400">
        {{ t('common.placeholder.description') }}
      </p>

      <div class="space-y-1 text-xs text-slate-500">
        <div v-if="subject.datasource_name" class="truncate">
          <span class="text-slate-400">{{ t('datasource.connectionName') }}:</span>
          <span class="ml-1">{{ subject.datasource_name }}</span>
        </div>
        <div v-if="subject.table_count !== undefined">
          <span class="text-slate-400">{{ t('tableInfo.title') }}:</span>
          <span class="ml-1">{{ subject.table_count }}</span>
        </div>
        <div v-if="subject.updated_at">
          <span class="text-slate-400">{{ t('common.updatedAt') }}:</span>
          <span class="ml-1">{{ formatDateTime(subject.updated_at) }}</span>
        </div>
      </div>
    </template>
  </el-card>
</template>

<style scoped>
.subject-card {
  width: 100%;
}
</style>
