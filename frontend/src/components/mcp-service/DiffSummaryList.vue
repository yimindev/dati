<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

export interface DiffSummaryItem {
  label: string;
  detail?: string;
  added?: string[];
  modified?: string[];
  deleted?: string[];
}

const props = withDefaults(
  defineProps<{
    items: DiffSummaryItem[];
    /** 超过该数量时截断并提示剩余项数；0 / 不传 = 全部展示 */
    limit?: number;
    /** 可选标题（带分隔线），popover 场景使用 */
    title?: string;
  }>(),
  { limit: 0, title: "" },
);

const { t } = useI18n();

const visibleItems = computed(() => {
  if (!props.limit || props.limit <= 0 || props.items.length <= props.limit) {
    return { list: props.items, extra: 0 };
  }
  return { list: props.items.slice(0, props.limit), extra: props.items.length - props.limit };
});
</script>

<template>
  <div class="diff-summary-list flex flex-col gap-2 max-h-[60vh] overflow-y-auto text-xs">
    <div
      v-if="title"
      class="font-semibold text-[var(--ep-text-color-primary)] mb-1 pb-1 border-b border-[var(--ep-border-color-lighter)]"
    >
      {{ title }}
    </div>
    <div
      v-for="item in visibleItems.list"
      :key="item.label"
      class="flex flex-col gap-1 pb-1 border-b border-[var(--ep-border-color-lighter)] last:border-0"
    >
      <div class="flex items-center justify-between font-medium">
        <span>{{ item.label }}</span>
        <span v-if="item.detail" class="text-[var(--ep-text-color-secondary)]">{{ item.detail }}</span>
      </div>
      <div v-if="item.added?.length" class="flex items-center gap-1 flex-wrap pl-1">
        <span class="text-[var(--ep-color-success)] text-[11px] font-medium">
          + {{ t("mcpService.changeAdded") }}:
        </span>
        <el-tag
          v-for="name in item.added"
          :key="name"
          type="success"
          size="small"
          effect="light"
          class="font-mono text-[11px] h-5 px-1.5"
        >
          {{ name }}
        </el-tag>
      </div>
      <div v-if="item.modified?.length" class="flex items-center gap-1 flex-wrap pl-1">
        <span class="text-[var(--ep-color-warning)] text-[11px] font-medium">
          ~ {{ t("mcpService.changeModified") }}:
        </span>
        <el-tag
          v-for="name in item.modified"
          :key="name"
          type="warning"
          size="small"
          effect="light"
          class="font-mono text-[11px] h-5 px-1.5"
        >
          {{ name }}
        </el-tag>
      </div>
      <div v-if="item.deleted?.length" class="flex items-center gap-1 flex-wrap pl-1">
        <span class="text-[var(--ep-color-danger)] text-[11px] font-medium">
          - {{ t("mcpService.changeDeleted") }}:
        </span>
        <el-tag
          v-for="name in item.deleted"
          :key="name"
          type="danger"
          size="small"
          effect="light"
          class="font-mono text-[11px] h-5 px-1.5"
        >
          {{ name }}
        </el-tag>
      </div>
    </div>
    <div
      v-if="visibleItems.extra > 0"
      class="text-[11px] text-[var(--ep-text-color-secondary)] pl-1"
    >
      {{ t("mcpService.moreChanges", { count: visibleItems.extra }) }}
    </div>
  </div>
</template>
