<script lang="ts" setup>
import {
  Menu as IconMenu,
} from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

// const isCollapse = ref(true)
function handleOpen(key: string, keyPath: string[]) {
  // eslint-disable-next-line no-console
  console.log(key, keyPath);
}
function handleClose(key: string, keyPath: string[]) {
  // eslint-disable-next-line no-console
  console.log(key, keyPath);
}
</script>

<template>
  <el-menu
    router
    default-active="1"
    class="el-menu-vertical-demo"
    @open="handleOpen"
    @close="handleClose"
  >
    <el-sub-menu index="1">
      <template #title>
        <el-icon>
          <span class="icon-[codicon--mcp]"></span>
        </el-icon>
        <span> {{ t('side.mcpBuilder') }} </span>
      </template>
    </el-sub-menu>
    <el-menu-item index="/nav/2">
      <el-icon>
        <IconMenu />
      </el-icon>
      <template #title> {{ t('side.sematicModels') }} </template>
    </el-menu-item>
    <el-menu-item index="/nav/4">
      <el-icon>
        <span class="icon-[codicon--database]"></span>
      </el-icon>
      <template #title> {{ t('side.dataSources') }} </template>
    </el-menu-item>
  </el-menu>
</template>
