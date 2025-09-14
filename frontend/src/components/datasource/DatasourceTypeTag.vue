<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type: string
}

const props = defineProps<Props>()

// 获取标签类型样式
const tagType = computed(() => {
  const typeMap: Record<string, string> = {
    'MySQL': 'success',
    'PostgreSQL': 'primary',
    'Oracle': 'warning',
    'SQL Server': 'info',
  }
  return typeMap[props.type] || 'default'
})
</script>

<template>
  <el-tag :type="tagType">{{ type }}</el-tag>
</template>
