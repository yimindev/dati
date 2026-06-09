<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import BaseHeader from "~/components/layouts/BaseHeader.vue";
import BaseSide from "~/components/layouts/BaseSide.vue";
import { useSystemStore } from "~/stores/system";
import { useAuthStore } from "~/stores/auth";

const systemStore = useSystemStore();
const authStore = useAuthStore();
const route = useRoute();
const { t } = useI18n();

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
    <a href="#main-content" class="skip-link">{{ t('common.skipToContent') }}</a>
    <template v-if="isAuthPage">
      <RouterView />
    </template>
    <template v-else>
      <BaseHeader />
      <div class="main-container flex">
        <BaseSide/>
        <div class="flex-1 min-w-0 bg-[var(--ep-fill-color-light)]">
          <div class="m-4 p-2 bg-[var(--ep-bg-color)]">
            <RouterView id="main-content" tabindex="-1" />
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
