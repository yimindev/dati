<script setup lang="ts">
import { useI18n } from "vue-i18n";

const { t } = useI18n();

interface Props {
  loading?: boolean;
  total: number;
  page: number;
  pageSize: number;
  pageSizes?: number[];
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  pageSizes: () => [10, 20, 50, 100],
});

const emit = defineEmits<{
  (e: "pageChange", page: number): void;
  (e: "pageSizeChange", size: number): void;
}>();
</script>

<template>
  <div v-loading="props.loading" class="data-table-shell">
    <div class="data-table-body">
      <slot />
    </div>

    <div v-if="total > 0" class="data-table-footer py-3.5 px-4">
      <span class="total-text">
        {{ t("common.total", { total }) }}
      </span>
      <el-pagination
        layout="sizes, prev, pager, next"
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
  overflow: hidden;
  border: 1px solid var(--ep-border-color-lighter);
  border-radius: 8px;
  background: var(--ep-bg-color);
}

.data-table-body :deep(.el-table) {
  --el-table-border-color: transparent;
}

.data-table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  /* padding via Tailwind py-3.5 px-4 on template */
  border-top: 1px solid var(--ep-border-color-lighter);
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
