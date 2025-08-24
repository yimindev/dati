<script lang="ts" setup>
import { isDark, toggleDark } from "~/composables";
import { Moon, Sunny, User } from "@element-plus/icons-vue";
</script>

<template>
  <el-menu class="el-menu-demo" mode="horizontal" :ellipsis="false" router>
    <el-menu-item index="/">
      <div class="flex items-center justify-center gap-2">
        <el-icon class="text-xl">
          <img src="/dcai.svg" alt="dcai" class="w-[1em] h-[1em]" />
        </el-icon>
        <span>DataConnAI</span>
      </div>
    </el-menu-item>
    <el-sub-menu index="2">
      <template #title> Workspace </template>
      <el-menu-item index="2-1"> item one </el-menu-item>
      <el-menu-item index="2-2"> item two </el-menu-item>
      <el-menu-item index="2-3"> item three </el-menu-item>
      <el-sub-menu index="2-4">
        <template #title> item four </template>
        <el-menu-item index="2-4-1"> item one </el-menu-item>
        <el-menu-item index="2-4-2"> item two </el-menu-item>
        <el-menu-item index="2-4-3"> item three </el-menu-item>
      </el-sub-menu>
    </el-sub-menu>
    <el-menu-item index="3" disabled> Info </el-menu-item>
    <el-menu-item index="4"> Orders </el-menu-item>

    <el-menu-item class="h-full" @click="toggleDark()">
      <button
        class="w-full cursor-pointer border-0 bg-transparent"
        style="height: var(--ep-menu-item-height)"
      >
          <el-icon class="inline-flex">
            <component :is="isDark ? Moon : Sunny" />
          </el-icon>
      </button>
    </el-menu-item>

    <el-menu-item class="h-full">
      <el-avatar :size="32" :icon="User" />
    </el-menu-item>
  </el-menu>
</template>

<style lang="scss">
.el-menu-demo {
  &.ep-menu--horizontal > .ep-menu-item:nth-child(1) {
    margin-right: auto;
  }
}
</style>
