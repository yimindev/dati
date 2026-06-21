<script setup lang="ts">
import { computed } from "vue";
import { WarningFilled, InfoFilled } from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const props = withDefaults(defineProps<{
  allowSelect: boolean;
  allowInsert: boolean;
  allowUpdate: boolean;
  allowDelete: boolean;
  allowDdl: boolean;
  allowMetadata: boolean;
  allowTransaction: boolean;
  allowSet: boolean;
  allowMulti?: boolean;
  maxRows: number;
  timeout: number;
  confirmRequired: boolean;
  showMulti?: boolean;
  showAnnotations?: boolean;
}>(), {
  allowMetadata: false,
  allowTransaction: false,
  allowSet: false,
  allowMulti: false,
  showMulti: false,
  showAnnotations: false,
});

const emit = defineEmits<{
  (e: "update:allowSelect", v: boolean): void;
  (e: "update:allowInsert", v: boolean): void;
  (e: "update:allowUpdate", v: boolean): void;
  (e: "update:allowDelete", v: boolean): void;
  (e: "update:allowDdl", v: boolean): void;
  (e: "update:allowMetadata", v: boolean): void;
  (e: "update:allowTransaction", v: boolean): void;
  (e: "update:allowSet", v: boolean): void;
  (e: "update:allowMulti", v: boolean): void;
  (e: "update:maxRows", v: number): void;
  (e: "update:timeout", v: number): void;
  (e: "update:confirmRequired", v: boolean): void;
}>();

const ops = computed(() => {
  const base = [
    { key: "allowSelect", label: "SELECT" },
    { key: "allowInsert", label: "INSERT" },
    { key: "allowUpdate", label: "UPDATE" },
    { key: "allowDelete", label: "DELETE" },
    { key: "allowDdl", label: "DDL", tooltip: t("mcpService.tool.allowDdlTooltip") },
    { key: "allowMetadata", label: "METADATA", tooltip: t("mcpService.tool.allowMetadataTooltip") },
    { key: "allowTransaction", label: "TRANSACTION", tooltip: t("mcpService.tool.allowTransactionTooltip") },
    { key: "allowSet", label: "SET", tooltip: t("mcpService.tool.allowSetTooltip") },
  ];
  if (props.showMulti) {
    base.push({ key: "allowMulti", label: "MULTI", tooltip: t("mcpService.tool.allowMultiTooltip") });
  }
  return base;
});

const toggleOp = (key: string) => {
  const map: Record<string, () => void> = {
    allowSelect: () => emit("update:allowSelect", !props.allowSelect),
    allowInsert: () => emit("update:allowInsert", !props.allowInsert),
    allowUpdate: () => emit("update:allowUpdate", !props.allowUpdate),
    allowDelete: () => emit("update:allowDelete", !props.allowDelete),
    allowDdl: () => emit("update:allowDdl", !props.allowDdl),
    allowMetadata: () => emit("update:allowMetadata", !props.allowMetadata),
    allowTransaction: () => emit("update:allowTransaction", !props.allowTransaction),
    allowSet: () => emit("update:allowSet", !props.allowSet),
    allowMulti: () => emit("update:allowMulti", !props.allowMulti),
  };
  map[key]?.();
};

const annotations = computed(() => {
  const isReadOnly = props.allowSelect && !props.allowInsert && !props.allowUpdate && !props.allowDelete && !props.allowDdl;
  const isDestructive = props.allowInsert || props.allowUpdate || props.allowDelete || props.allowDdl;
  return {
    readOnlyHint: isReadOnly,
    destructiveHint: isDestructive,
    idempotentHint: isReadOnly,
  };
});
</script>

<template>
  <div class="flex flex-col gap-4">
    <!-- Allowed Operations -->
    <div>
      <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.allowedOps") }}</label>
      <div class="flex flex-wrap gap-2">
        <el-tooltip
          v-for="op in ops"
          :key="op.key"
          :content="op.tooltip"
          :disabled="!op.tooltip"
          placement="top"
        >
          <button
            type="button"
            class="perm-pill"
            :class="{ active: (props as any)[op.key] }"
            @click="toggleOp(op.key)"
          >
            {{ op.label }}
          </button>
        </el-tooltip>
      </div>
      <p class="flex items-start gap-1 mt-2 text-xs text-[var(--ep-color-warning)]">
        <el-icon><WarningFilled /></el-icon>
        {{ t("mcpService.tool.sqlRiskWarning") }}
      </p>
    </div>

    <!-- Numeric Limits -->
    <div class="flex items-start gap-8">
      <div class="w-44">
        <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.maxRows") }}</label>
        <el-input-number
          :model-value="maxRows"
          :min="1"
          :max="100000"
          size="small"
          class="w-full"
          @update:model-value="emit('update:maxRows', $event ?? 1000)"
        />
      </div>
      <div class="w-44">
        <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.timeout") }} (s)</label>
        <el-input-number
          :model-value="timeout"
          :min="1"
          :max="300"
          size="small"
          class="w-full"
          @update:model-value="emit('update:timeout', $event ?? 30)"
        />
      </div>
    </div>

    <!-- Confirm Required -->
    <div>
      <el-checkbox
        :model-value="confirmRequired"
        @update:model-value="emit('update:confirmRequired', !!$event)"
      >
        {{ t("mcpService.tool.confirmRequired") }}
      </el-checkbox>
    </div>

    <!-- Annotations Preview -->
    <div v-if="showAnnotations">
      <label class="block mb-1.5 text-[var(--ep-text-color-primary)]">{{ t("mcpService.tool.annotationsPreview") }}</label>
      <div class="bg-[var(--ep-fill-color-lighter)] px-5 py-4 rounded-lg border border-[var(--ep-border-color-lighter)]">
        <div class="grid grid-cols-3 gap-x-4 gap-y-2">
          <div v-for="(val, key) in annotations" :key="key" class="flex items-center gap-2">
            <span class="text-xs font-mono text-[var(--ep-text-color-secondary)] shrink-0">{{ key }}:</span>
            <el-tag :type="val ? 'success' : 'info'" size="small" effect="plain">
              {{ val }}
            </el-tag>
          </div>
        </div>
        <p class="text-[10px] text-[var(--ep-text-color-placeholder)] mt-3 flex items-center gap-1">
          <el-icon><InfoFilled /></el-icon>
          {{ t("mcpService.tool.annotationsHint") }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── SQL 权限按钮（需 JS 切换 .active 类 + hover 交互） ── */
.perm-pill {
  padding: 4px 12px;
  border: 1.5px solid var(--ep-border-color);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  background: var(--ep-bg-color);
  color: var(--ep-text-color-regular);
  transition: all 0.15s;
}
.perm-pill:hover {
  border-color: var(--ep-color-primary);
}
.perm-pill.active {
  background: var(--ep-color-primary-light-9);
  border-color: var(--ep-color-primary);
  color: var(--ep-color-primary);
  font-weight: 600;
}
</style>
