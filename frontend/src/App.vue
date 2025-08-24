<script setup lang="ts">
import BaseHeader from "~/components/layouts/BaseHeader.vue";
import BaseSide from "~/components/layouts/BaseSide.vue";
</script>

<template>
  <el-config-provider namespace="ep">
    <BaseHeader />
    <div class="main-container flex">
      <BaseSide />
      <div class="w-full py-4">
        <RouterView />
      </div>
    </div>
  </el-config-provider>
</template>

<style>
#app {
  text-align: center;
  color: var(--ep-text-color-primary);
}

.main-container {
  height: calc(100vh - var(--ep-menu-item-height) - 4px);
}
</style>
