<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { CircleCheckFilled, CircleCloseFilled } from "@element-plus/icons-vue";
import type { MetadataUpdateData } from "~/api/mcp-tool-test";

defineProps<{ data: MetadataUpdateData }>();

const { t } = useI18n();
</script>

<template>
  <template v-for="r in data.results" :key="r.entity">
    <div
      class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3"
      :class="{ 'result-failed': !r.success }"
    >
      <div
        class="result-card-header flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-2 px-3"
        :class="{ 'header-failed': !r.success }"
      >
        <el-icon v-if="r.success" class="text-[var(--ep-color-success)]"
          ><CircleCheckFilled
        /></el-icon>
        <el-icon v-else class="text-[var(--ep-color-danger)]"
          ><CircleCloseFilled
        /></el-icon>
        <span
          >{{ t(`mcpService.toolTest.entityType.${r.entity_type}`) }}:
          {{ r.entity }}</span
        >
        <el-tag v-if="r.success" size="small" class="ml-auto">
          {{
            r.change_type === "CREATE"
              ? t("mcpService.toolTest.changeCreate")
              : t("mcpService.toolTest.changeUpdate")
          }}
        </el-tag>
      </div>
      <div
        v-if="r.success"
        class="result-card-body p-3 flex flex-col gap-2.5 text-[13px]"
      >
        <div v-if="r.old">
          <div class="text-xs text-[var(--ep-text-color-placeholder)] mb-1">
            {{ t("mcpService.toolTest.oldValue") }}
          </div>
          <p
            v-if="r.old.description"
            class="text-[var(--ep-text-color-secondary)]"
          >
            {{ r.old.description }}
          </p>
          <div v-if="r.old.aliases?.length" class="flex flex-wrap gap-1">
            <el-tag
              v-for="a in r.old.aliases"
              :key="a"
              size="small"
              type="info"
              >{{ a }}</el-tag
            >
          </div>
        </div>
        <div>
          <div class="text-xs text-[var(--ep-text-color-placeholder)] mb-1">
            {{ t("mcpService.toolTest.newValue") }}
          </div>
          <p
            v-if="r.new?.description"
            class="text-[var(--ep-text-color-primary)]"
          >
            {{ r.new.description }}
          </p>
          <div v-if="r.new?.aliases?.length" class="flex flex-wrap gap-1">
            <el-tag v-for="a in r.new.aliases" :key="a" size="small"
              >{{ a }}</el-tag
            >
          </div>
        </div>
      </div>
      <div
        v-else
        class="result-error py-3 px-4 text-[13px] text-[var(--ep-color-danger)] font-mono bg-[var(--ep-color-danger-light-9)]"
      >
        {{ r.error?.error_category }}: {{ r.error?.message }}
      </div>
    </div>
  </template>
  <el-empty
    v-if="data.results.length === 0"
    :description="t('mcpService.toolTest.emptyResult')"
  />
</template>

<style scoped>
/* ── Result card state variants (requires dynamic :class) ── */
.result-failed {
  border-color: var(--ep-color-danger-light-7);
}
.header-failed {
  background: var(--ep-color-danger-light-9);
  color: var(--ep-color-danger);
}
</style>
