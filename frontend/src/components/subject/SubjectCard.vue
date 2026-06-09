<script setup lang="ts">
import type { SubjectVO } from '~/api/subject'
import { formatDateTime } from '~/composables'
import { MoreFilled } from '@element-plus/icons-vue'
import { computed } from 'vue'
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

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visibleAliases = computed(() => props.subject.aliases?.slice(0, 2) ?? [])

const hiddenAliasCount = computed(() => {
  const totalAliases = props.subject.aliases?.length ?? 0
  return Math.max(totalAliases - visibleAliases.value.length, 0)
})

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
    class="w-full cursor-pointer transition-all duration-200 hover:shadow-lg [&_.el-card__header]:!px-3 [&_.el-card__header]:!py-1.5 [&_.el-card__body]:!px-3 [&_.el-card__body]:!pt-2 [&_.el-card__body]:!pb-3"
    shadow="hover"
    @click="handleCardClick"
  >
    <template #header>
      <div class="flex items-center justify-between gap-1.5">
        <div class="min-w-0 flex flex-1 items-center gap-1.5 overflow-hidden">
          <div class="shrink min-w-0 truncate text-sm font-medium text-[var(--ep-text-color-primary)]">{{ subject.name }}</div>
          <div
            v-if="subject.aliases?.length"
            class="min-w-0 flex shrink items-center gap-1 overflow-hidden"
          >
            <span
              v-for="alias in visibleAliases"
              :key="alias"
              class="min-w-0 max-w-full truncate rounded-full bg-[var(--ep-fill-color-light)] px-1.5 py-0 text-xs text-[var(--ep-text-color-regular)]"
            >
              {{ alias }}
            </span>
            <span
              v-if="hiddenAliasCount > 0"
              class="shrink-0 rounded-full bg-[var(--ep-fill-color-light)] px-1.5 py-0 text-xs text-[var(--ep-text-color-secondary)]"
            >
              +{{ hiddenAliasCount }}
            </span>
          </div>
        </div>
        <el-dropdown trigger="click" data-stop-card-click="true">
          <el-tooltip :content="t('common.actions')" placement="top">
            <el-button
              text
              size="small"
              :icon="MoreFilled"
              class="!m-0 !px-1 !py-0.5"
              data-stop-card-click="true"
            />
          </el-tooltip>
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
        class="mb-1 line-clamp-3 min-h-[3.75rem] text-sm text-[var(--ep-text-color-regular)]"
      >
        {{ subject.description }}
      </p>
      <p v-else class="mb-1 min-h-[3.75rem] text-sm text-[var(--ep-text-color-placeholder)]">
        -
      </p>

      <div class="space-y-1 text-xs text-[var(--ep-text-color-secondary)]">
        <div v-if="subject.id" class="truncate" :title="subject.id">
          <span class="text-[var(--ep-text-color-secondary)]">ID:</span>
          <span class="ml-1 font-mono">{{ subject.id }}</span>
        </div>
        <div v-if="subject.datasource_name" class="truncate">
          <span class="text-[var(--ep-text-color-secondary)]">{{ t('datasource.connectionName') }}:</span>
          <span class="ml-1">{{ subject.datasource_name }}</span>
        </div>
        <div v-if="subject.table_count !== undefined">
          <span class="text-[var(--ep-text-color-secondary)]">{{ t('tableInfo.title') }}:</span>
          <span class="ml-1">{{ subject.table_count }}</span>
        </div>
        <div v-if="subject.updated_at">
          <span class="text-[var(--ep-text-color-secondary)]">{{ t('common.updatedAt') }}:</span>
          <span class="ml-1">{{ formatDateTime(subject.updated_at) }}</span>
        </div>
      </div>
    </template>
  </el-card>
</template>
