<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { CircleCheckFilled, CircleCloseFilled } from "@element-plus/icons-vue";
import type { MetadataUpdateData, MetadataUpdateResult as ResultItem } from "~/api/mcp-tool-test";

const props = defineProps<{ data: MetadataUpdateData }>();

const { t } = useI18n();

const successCount = computed(
  () => props.data.results.filter((r) => r.success).length,
);
const failedCount = computed(
  () => props.data.results.filter((r) => !r.success).length,
);

const isDescModified = (oldDesc?: string, newDesc?: string) =>
  (oldDesc ?? "") !== (newDesc ?? "");

const aliasesDiff = (r: ResultItem) => {
  const oldList = r.old?.aliases ?? [];
  const newList = r.new?.aliases ?? [];
  const oldSet = new Set(oldList);
  const newSet = new Set(newList);

  const added = newList.filter((a) => !oldSet.has(a));
  const removed = oldList.filter((a) => !newSet.has(a));
  const isModified = added.length > 0 || removed.length > 0;

  return { added, removed, isModified };
};

const isItemUnchanged = (r: ResultItem) => {
  if (r.change_type !== "UPDATE") return false;
  const descUnchanged = !isDescModified(r.old?.description, r.new?.description);
  const aliasesUnchanged = !aliasesDiff(r).isModified;
  return descUnchanged && aliasesUnchanged;
};
</script>

<template>
  <div>
    <!-- Summary bar -->
    <div
      v-if="data.results.length > 0"
      class="summary-bar mb-3 flex items-center gap-2 flex-wrap text-xs text-[var(--ep-text-color-secondary)]"
    >
      <span class="font-semibold text-[var(--ep-text-color-primary)]">
        {{ t("mcpService.toolTest.summaryTotal", { total: data.results.length }) }}
      </span>
      <el-tag v-if="successCount > 0" size="small" type="success" effect="light">
        {{ t("mcpService.toolTest.summarySuccess", { count: successCount }) }}
      </el-tag>
      <el-tag v-if="failedCount > 0" size="small" type="danger" effect="light">
        {{ t("mcpService.toolTest.summaryFailed", { count: failedCount }) }}
      </el-tag>
    </div>

    <!-- Result cards -->
    <template v-for="r in data.results" :key="r.entity">
      <div
        class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3 bg-[var(--ep-bg-color)]"
        :class="{ 'result-failed': !r.success }"
      >
        <!-- Header -->
        <div
          class="result-card-header flex items-center gap-2 text-xs py-2 px-3 bg-[var(--ep-fill-color)] border-b border-[var(--ep-border-color-lighter)]"
          :class="{ 'header-failed': !r.success }"
        >
          <el-icon v-if="r.success" class="text-[var(--ep-color-success)] text-sm">
            <CircleCheckFilled />
          </el-icon>
          <el-icon v-else class="text-[var(--ep-color-danger)] text-sm">
            <CircleCloseFilled />
          </el-icon>

          <el-tag size="small" type="info" effect="plain" class="font-semibold shrink-0">
            {{ t(`mcpService.toolTest.entityType.${r.entity_type}`) }}
          </el-tag>

          <code class="entity-name font-mono font-semibold text-[var(--ep-text-color-primary)] truncate">
            {{ r.entity }}
          </code>

          <div class="ml-auto flex items-center gap-1.5 shrink-0">
            <el-tag v-if="!r.success" size="small" type="danger">
              {{ t("mcpService.toolTest.failed") }}
            </el-tag>
            <el-tag v-else-if="r.change_type === 'CREATE'" size="small" type="success">
              {{ t("mcpService.toolTest.changeCreate") }}
            </el-tag>
            <el-tag v-else-if="isItemUnchanged(r)" size="small" type="info">
              {{ t("mcpService.toolTest.unchanged") }}
            </el-tag>
            <el-tag v-else size="small" type="primary">
              {{ t("mcpService.toolTest.changeUpdate") }}
            </el-tag>
          </div>
        </div>

        <!-- Failure body -->
        <div v-if="!r.success" class="p-3 bg-[var(--ep-color-danger-light-9)]">
          <div v-if="r.error?.error_category" class="flex items-center gap-1.5 mb-1.5">
            <el-tag size="small" type="danger" effect="dark">{{ r.error.error_category }}</el-tag>
          </div>
          <pre class="text-xs font-mono text-[var(--ep-color-danger)] m-0 whitespace-pre-wrap break-words bg-[var(--ep-bg-color)] p-2.5 rounded border border-[var(--ep-color-danger-light-7)]">{{ r.error?.message }}</pre>
        </div>

        <!-- CREATE body -->
        <div
          v-else-if="r.change_type === 'CREATE'"
          class="p-3 flex flex-col gap-2.5 text-[13px] bg-[var(--ep-bg-color)]"
        >
          <div class="flex items-start gap-3">
            <span class="w-16 shrink-0 text-xs font-medium text-[var(--ep-text-color-placeholder)] mt-0.5">
              {{ t("mcpService.toolTest.fieldDescription") }}
            </span>
            <span v-if="r.new?.description" class="text-[var(--ep-text-color-primary)]">
              {{ r.new.description }}
            </span>
            <span v-else class="text-[var(--ep-text-color-placeholder)] italic text-xs">
              {{ t("mcpService.toolTest.noDescription") }}
            </span>
          </div>
          <div class="flex items-start gap-3">
            <span class="w-16 shrink-0 text-xs font-medium text-[var(--ep-text-color-placeholder)] mt-0.5">
              {{ t("mcpService.toolTest.fieldAliases") }}
            </span>
            <div v-if="r.new?.aliases?.length" class="flex flex-wrap gap-1">
              <el-tag v-for="a in r.new.aliases" :key="a" size="small">{{ a }}</el-tag>
            </div>
            <span v-else class="text-[var(--ep-text-color-placeholder)] italic text-xs">
              {{ t("mcpService.toolTest.noAliases") }}
            </span>
          </div>
        </div>

        <!-- UPDATE body (Diff comparison) -->
        <div
          v-else
          class="p-3 flex flex-col gap-3 text-[13px] bg-[var(--ep-bg-color)]"
        >
          <!-- Description diff -->
          <div class="diff-section border border-[var(--ep-border-color-lighter)] rounded p-2.5 bg-[var(--ep-fill-color-lighter)]">
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-xs font-semibold text-[var(--ep-text-color-secondary)]">
                {{ t("mcpService.toolTest.fieldDescription") }}
              </span>
              <el-tag
                size="small"
                :type="isDescModified(r.old?.description, r.new?.description) ? 'warning' : 'info'"
                effect="plain"
              >
                {{
                  isDescModified(r.old?.description, r.new?.description)
                    ? t("mcpService.toolTest.fieldModified")
                    : t("mcpService.toolTest.fieldUnchanged")
                }}
              </el-tag>
            </div>

            <div
              v-if="isDescModified(r.old?.description, r.new?.description)"
              class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs"
            >
              <div class="diff-box p-2 rounded bg-[var(--ep-bg-color)] border border-[var(--ep-border-color-lighter)]">
                <div class="text-[11px] font-medium text-[var(--ep-text-color-placeholder)] mb-1">
                  {{ t("mcpService.toolTest.oldValue") }}
                </div>
                <p v-if="r.old?.description" class="m-0 text-[var(--ep-text-color-secondary)] leading-5">{{ r.old.description }}</p>
                <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noDescription") }}</span>
              </div>
              <div class="diff-box p-2 rounded bg-[var(--ep-bg-color)] border border-[var(--ep-color-primary-light-8)]">
                <div class="text-[11px] font-medium text-[var(--ep-color-primary)] mb-1">
                  {{ t("mcpService.toolTest.newValue") }}
                </div>
                <p v-if="r.new?.description" class="m-0 text-[var(--ep-text-color-primary)] leading-5">{{ r.new.description }}</p>
                <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noDescription") }}</span>
              </div>
            </div>
            <div v-else class="text-xs text-[var(--ep-text-color-primary)] px-1">
              <span v-if="r.new?.description">{{ r.new.description }}</span>
              <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noDescription") }}</span>
            </div>
          </div>

          <!-- Aliases diff -->
          <div class="diff-section border border-[var(--ep-border-color-lighter)] rounded p-2.5 bg-[var(--ep-fill-color-lighter)]">
            <div class="flex items-center justify-between mb-1.5">
              <span class="text-xs font-semibold text-[var(--ep-text-color-secondary)]">
                {{ t("mcpService.toolTest.fieldAliases") }}
              </span>
              <el-tag
                size="small"
                :type="aliasesDiff(r).isModified ? 'warning' : 'info'"
                effect="plain"
              >
                {{
                  aliasesDiff(r).isModified
                    ? t("mcpService.toolTest.fieldModified")
                    : t("mcpService.toolTest.fieldUnchanged")
                }}
              </el-tag>
            </div>

            <div
              v-if="aliasesDiff(r).isModified"
              class="grid grid-cols-1 md:grid-cols-2 gap-2 text-xs"
            >
              <div class="diff-box p-2 rounded bg-[var(--ep-bg-color)] border border-[var(--ep-border-color-lighter)]">
                <div class="text-[11px] font-medium text-[var(--ep-text-color-placeholder)] mb-1">
                  {{ t("mcpService.toolTest.oldValue") }}
                </div>
                <div v-if="r.old?.aliases?.length" class="flex flex-wrap gap-1">
                  <el-tag v-for="a in r.old.aliases" :key="a" size="small" type="info">{{ a }}</el-tag>
                </div>
                <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noAliases") }}</span>
              </div>
              <div class="diff-box p-2 rounded bg-[var(--ep-bg-color)] border border-[var(--ep-color-primary-light-8)]">
                <div class="text-[11px] font-medium text-[var(--ep-color-primary)] mb-1">
                  {{ t("mcpService.toolTest.newValue") }}
                </div>
                <div v-if="r.new?.aliases?.length" class="flex flex-wrap gap-1">
                  <el-tag
                    v-for="a in r.new.aliases"
                    :key="a"
                    size="small"
                    :type="aliasesDiff(r).added.includes(a) ? 'success' : undefined"
                  >
                    <span v-if="aliasesDiff(r).added.includes(a)" class="font-bold mr-0.5">+</span>{{ a }}
                  </el-tag>
                </div>
                <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noAliases") }}</span>
              </div>
            </div>
            <div v-else class="text-xs px-1">
              <div v-if="r.new?.aliases?.length" class="flex flex-wrap gap-1">
                <el-tag v-for="a in r.new.aliases" :key="a" size="small">{{ a }}</el-tag>
              </div>
              <span v-else class="text-[var(--ep-text-color-placeholder)] italic">{{ t("mcpService.toolTest.noAliases") }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty
      v-if="data.results.length === 0"
      :description="t('mcpService.toolTest.emptyResult')"
    />
  </div>
</template>

<style scoped>
.result-failed {
  border-color: var(--ep-color-danger-light-7);
}
.header-failed {
  background: var(--ep-color-danger-light-9);
  color: var(--ep-color-danger);
}
.entity-name {
  background: var(--ep-fill-color-darker);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
}
</style>

