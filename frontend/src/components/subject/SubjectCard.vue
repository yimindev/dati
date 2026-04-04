<script setup lang="ts">
import type { SubjectVO } from '~/api/subject'
import { formatDateTime } from '~/composables'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  subject: SubjectVO
}

interface Emits {
  (e: 'edit', subject: SubjectVO): void
  (e: 'delete', subject: SubjectVO): void
  (e: 'click', subject: SubjectVO): void
}

defineProps<Props>()
defineEmits<Emits>()
</script>

<template>
  <el-card
    class="subject-card cursor-pointer transition-all duration-200 hover:-translate-y-0.5"
    shadow="hover"
    @click="$emit('click', subject)"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <span class="font-medium">{{ subject.name }}</span>
        <div class="flex gap-1" @click.stop>
          <el-button type="primary" link @click="$emit('edit', subject)">
            {{ t('common.edit') }}
          </el-button>
          <el-button type="danger" link @click="$emit('delete', subject)">
            {{ t('common.delete') }}
          </el-button>
        </div>
      </div>
    </template>

    <template #default>
      <p
        v-if="subject.description"
        class="text-gray-600 text-sm line-clamp-2 mb-3"
      >
        {{ subject.description }}
      </p>
      <p v-else class="text-gray-400 text-sm italic mb-3">
        {{ t('common.placeholder.description') }}
      </p>

      <div class="text-xs text-gray-500 space-y-1">
        <div v-if="subject.datasource_name">
          <span class="text-gray-400">{{ t('datasource.connectionName') }}:</span>
          {{ subject.datasource_name }}
        </div>
        <div v-if="subject.table_count !== undefined">
          <span class="text-gray-400">{{ t('tableInfo.title') }}:</span>
          {{ subject.table_count }}
        </div>
        <div v-if="subject.updated_at">
          <span class="text-gray-400">{{ t('common.updatedAt') }}:</span>
          {{ formatDateTime(subject.updated_at) }}
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
