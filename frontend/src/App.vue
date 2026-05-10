<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import BaseHeader from "~/components/layouts/BaseHeader.vue";
import BaseSide from "~/components/layouts/BaseSide.vue";
import { useSystemStore } from "~/stores/system";
import { useAuthStore } from "~/stores/auth";

const systemStore = useSystemStore();
const authStore = useAuthStore();
const route = useRoute();

const isAuthPage = computed(() => ["/login", "/register"].includes(route.path));

onMounted(() => {
  if (!isAuthPage.value) {
    systemStore.loadConfig();
    authStore.fetchUser();
  }
});
</script>

<template>
  <el-config-provider namespace="ep">
    <template v-if="isAuthPage">
      <RouterView />
    </template>
    <template v-else>
      <BaseHeader />
      <div class="main-container flex">
        <BaseSide/>
        <div class="flex-1 min-w-0 bg-[var(--ep-fill-color-light)]">
          <div class="m-4 p-2 bg-[var(--ep-bg-color)]">
            <RouterView />
          </div>
        </div>
      </div>
    </template>
  </el-config-provider>
</template>

<style>
.main-container {
  height: calc(100vh - var(--ep-menu-item-height) - 4px);
}
</style>
