<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

interface Props {
  loading?: boolean;
  total: number;
  page: number;
  pageSize: number;
  pageSizes?: number[];
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  pageSizes: () => [10, 20, 50, 100],
  compact: false,
});

const paginationLayout = computed(() =>
  props.pageSizes.length > 1 ? "sizes, prev, pager, next" : "prev, pager, next",
);

const emit = defineEmits<{
  (e: "pageChange", page: number): void;
  (e: "pageSizeChange", size: number): void;
}>();
</script>

<template>
  <div
    v-loading="props.loading"
    class="data-table-shell"
    :class="{ 'data-table-shell--compact': props.compact }"
  >
    <div class="data-table-body">
      <slot />
    </div>

    <div v-if="total > 0" class="data-table-footer py-3.5 px-4">
      <span class="total-text">
        {{ t("common.total", { total }) }}
      </span>
      <el-pagination
        :layout="paginationLayout"
        :disabled="props.loading"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        @current-change="(p: number) => emit('pageChange', p)"
        @size-change="(s: number) => emit('pageSizeChange', s)"
      />
    </div>
  </div>
</template>

<style scoped>
.data-table-shell {
  display: flex;
  flex-direction: column;
  flex-grow: 0;
  flex-shrink: 1;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}

.data-table-body {
  display: flex;
  flex-direction: column;
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
}

.data-table-body :deep(.el-table),
.data-table-body :deep(.ep-table) {
  --el-table-border-color: transparent;
  height: 100%;
}

.data-table-shell--compact {
  display: block;
  flex: initial;
}

.data-table-shell--compact .data-table-body {
  display: block;
  flex: initial;
  overflow: visible;
}

.data-table-shell--compact .data-table-body :deep(.el-table),
.data-table-shell--compact .data-table-body :deep(.ep-table) {
  height: auto;
}

.data-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  /* padding via Tailwind py-3.5 px-4 on template */
  border-top: 1px solid var(--ep-border-color-lighter);
  flex-shrink: 0;
}

.data-table-shell--compact .data-table-footer {
  padding: 10px 12px !important;
}

.total-text {
  color: var(--ep-text-color-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .data-table-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
