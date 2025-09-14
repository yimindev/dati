<script lang="ts" setup>
import {
  Menu as IconMenu,
  ArrowRight,
  ArrowLeft,
} from "@element-plus/icons-vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const collapsed = ref(false)

const route = useRoute()
const activeMenu = computed(() => {
  return (route.meta?.activeMenu as string) || route.path
})
</script>

<template>
  <div class="relative">
    <el-menu
      router
      :default-active="activeMenu"
      class="el-menu-vertical-demo h-full !border-r-0"
      :collapse="collapsed"
      :collapse-transition="false"
      :class="collapsed ? 'w-16' : 'w-50'"
    >
      <el-menu-item index="/nav/1/item-1">
          <el-icon>
            <span class="icon-[codicon--mcp]"></span>
          </el-icon>
        <template #title> {{ t('side.mcpBuilder') }} </template>
      </el-menu-item>
      <el-menu-item index="/nav/2">
        <el-icon>
          <IconMenu />
        </el-icon>
        <template #title> {{ t('side.sematicModels') }} </template>
      </el-menu-item>
      <el-menu-item index="/datasource">
        <el-icon>
          <span class="icon-[codicon--database]"></span>
        </el-icon>
        <template #title> {{ t('side.dataSources') }} </template>
      </el-menu-item>
    </el-menu>
    
    <!-- 展开/收起按钮 -->
    <button
      @click="collapsed = !collapsed"
      class="absolute top-1/2 left-full -translate-y-full w-3.5 h-12 rounded-r-xl bg-[var(--ep-bg-color)] cursor-pointer hover:bg-[var(--ep-menu-hover-bg-color)] transition-colors duration-300 ease-in-out)]"
      :title="collapsed ? t('side.expand') : t('side.collapse')"
    >
      <el-icon class="w-2 h-3 ">
        <ArrowRight v-if="collapsed" />
        <ArrowLeft v-else />
      </el-icon>
    </button>
  </div>
</template>
