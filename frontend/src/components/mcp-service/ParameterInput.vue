<script setup lang="ts">
defineProps<{
  parameter: { type?: string; description?: string };
  modelValue: any;
  size?: "small" | "default" | "large";
  placeholder?: string;
}>();

defineEmits<{ (e: "update:modelValue", v: any): void }>();
</script>

<template>
  <el-input
    v-if="parameter.type === 'String'"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :size="size"
    :placeholder="placeholder || parameter.description"
    class="w-full"
  />
  <el-input
    v-else-if="parameter.type === 'Number'"
    type="number"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event === '' ? undefined : Number($event))"
    :size="size"
    class="w-full"
  >
    <template #suffix>
      <span class="text-xs font-mono font-semibold text-[var(--ep-text-color-placeholder)]">#</span>
    </template>
  </el-input>
  <el-switch
    v-else-if="parameter.type === 'Boolean'"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
  />
  <el-date-picker
    v-else-if="parameter.type === 'DateTime'"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    type="datetime"
    class="w-full"
  />
  <el-input-tag
    v-else-if="parameter.type === 'Array'"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :size="size"
    :placeholder="$t('mcpService.toolTest.tagPlaceholder')"
    class="w-full"
  />
  <el-input
    v-else
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :size="size"
    :placeholder="placeholder || parameter.description"
    class="w-full"
  />
</template>
