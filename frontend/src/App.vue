<script setup lang="ts">
import BaseHeader from "~/components/layouts/BaseHeader.vue";
import BaseSide from "~/components/layouts/BaseSide.vue";

const isCollapse = ref(false)
</script>

<template>
  <el-config-provider namespace="ep">
    <BaseHeader
      :collapsed="isCollapse"
      @toggle-collapse="isCollapse = !isCollapse"
    />
    <div class="main-container flex">
      <BaseSide/>
      <div class="w-full bg-[var(--ep-fill-color-light)]">
        <div class="m-4 p-2 bg-[var(--ep-bg-color)]">
          <RouterView />
        </div>
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
