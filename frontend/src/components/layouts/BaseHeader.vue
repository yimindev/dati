<script lang="ts" setup>
import { isDark, toggleDark } from "~/composables";
import { Moon, Sunny, CircleClose, Key } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { setI18nLanguage, type AppLang } from "~/plugins/i18n";
import { useAuthStore } from "~/stores/auth";

const { t, locale } = useI18n();
const authStore = useAuthStore();
const router = useRouter();

const localeLabel = computed(() => (locale.value === "zh" ? "中文" : "EN"));
const userDisplayName = computed(() => authStore.user?.display_name || authStore.user?.name || "");

async function changeLocale(lang: AppLang) {
  await setI18nLanguage(lang);
}

function handleCommand(command: string) {
  if (command === "logout") {
    authStore.logout();
  } else if (command === "api-keys") {
    router.push("/settings/api-keys");
  }
}

function goToDocs() {
  window.open('/docs/', '_blank');
}
</script>

<template>
  <el-menu class="el-menu-demo" mode="horizontal" :ellipsis="false" :default-active="''">
    <el-menu-item index="/" @click="router.push('/')">
      <div class="flex items-center justify-center gap-2">
        <img src="/dati.svg" alt="dati" class="size-7 object-contain" />
        <span class="text-base font-semibold leading-none">
          {{ t("layout.header.brand") }}
        </span>
      </div>
    </el-menu-item>

    <el-sub-menu index="2">
      <template #title> {{ t("layout.header.workspace") }} </template>
      <el-menu-item index="2-1"> {{ t("layout.header.itemOne") }} </el-menu-item>
      <el-menu-item index="2-2"> {{ t("layout.header.itemTwo") }} </el-menu-item>
    </el-sub-menu>

    <el-menu-item index="header-theme" class="h-full" @click="toggleDark()">
      <button
        class="w-full cursor-pointer border-0 bg-transparent"
        style="height: var(--ep-menu-item-height)"
        :title="t('layout.header.theme')"
        :aria-label="t('layout.header.theme')"
      >
        <el-icon class="inline-flex">
          <component :is="isDark ? Moon : Sunny" />
        </el-icon>
      </button>
    </el-menu-item>

    <el-menu-item index="header-locale" class="h-full">
      <el-dropdown
        trigger="click"
        @command="(cmd: AppLang) => changeLocale(cmd)"
      >
        <span class="el-dropdown-link select-none">
          {{ localeLabel }}
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="en">English</el-dropdown-item>
            <el-dropdown-item command="zh">中文</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-menu-item>

    <el-menu-item index="header-docs" class="h-full" @click="goToDocs">
      <el-tooltip :content="t('layout.header.help')" placement="bottom">
        <el-icon class="inline-flex">
          <span class="icon-[codicon--question]"></span>
        </el-icon>
      </el-tooltip>
    </el-menu-item>

    <el-menu-item index="header-user" class="h-full" v-if="authStore.isLoggedIn">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="size-full flex items-center justify-center gap-2 cursor-pointer">
          <el-avatar :size="24">{{ userDisplayName.charAt(0).toUpperCase() }}</el-avatar>
          <span class="text-sm">{{ userDisplayName }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="api-keys">
              <el-icon><Key /></el-icon>
              <span>{{ t("apiKeys.title") }}</span>
            </el-dropdown-item>
            <el-dropdown-item command="logout">
              <el-icon><CircleClose /></el-icon>
              <span>{{ t("auth.logout") }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </el-menu-item>
    <el-menu-item index="header-login" class="h-full" v-else>
      <RouterLink to="/login">
        <el-button type="primary" size="small">{{ t("auth.login") }}</el-button>
      </RouterLink>
    </el-menu-item>
  </el-menu>
</template>

<style lang="scss">
.el-menu-demo {
  &.ep-menu--horizontal,
  &.el-menu--horizontal {
    > .ep-menu-item:nth-child(1),
    > .el-menu-item:nth-child(1) {
      margin-right: auto;
    }

    /* Make Header menu completely stateless: remove active bottom underline and active text highlight */
    > .ep-menu-item,
    > .el-menu-item,
    > .ep-sub-menu > .ep-sub-menu__title,
    > .el-sub-menu > .el-sub-menu__title {
      border-bottom: none !important;

      &.is-active {
        border-bottom: none !important;
        color: var(--ep-menu-text-color, var(--el-menu-text-color)) !important;
      }

      &:hover,
      &:focus,
      &:focus-visible {
        border-bottom: none !important;
        outline: none !important;
      }
    }
  }
}
</style>
