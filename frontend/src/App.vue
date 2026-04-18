<script setup lang="ts">
import { onMounted } from "vue";
import BaseHeader from "~/components/layouts/BaseHeader.vue";
import BaseSide from "~/components/layouts/BaseSide.vue";
import { useSystemStore } from "~/stores/system";

const isCollapse = ref(false);
const systemStore = useSystemStore();

onMounted(() => {
  systemStore.loadConfig();
});
</script>

<template>
  <el-config-provider namespace="ep">
    <BaseHeader
      :collapsed="isCollapse"
      @toggle-collapse="isCollapse = !isCollapse"
    />
    <div class="main-container flex">
      <BaseSide/>
      <div class="flex-1 min-w-0 bg-[var(--ep-fill-color-light)]">
        <div class="m-4 p-2 bg-[var(--ep-bg-color)]">
          <RouterView />
        </div>
      </div>
    </div>
  </el-config-provider>
</template>

<style>
.main-container {
  height: calc(100vh - var(--ep-menu-item-height) - 4px);
}
</style>
