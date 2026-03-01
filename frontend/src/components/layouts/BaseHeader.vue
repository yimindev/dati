<script lang="ts" setup>
import { isDark, toggleDark } from "~/composables";
import { Moon, Sunny } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { setI18nLanguage, type AppLang } from "~/plugins/i18n";

const { t, locale } = useI18n();

const localeLabel = computed(() => (locale.value === "zh" ? "中文" : "EN"));

async function changeLocale(lang: AppLang) {
  await setI18nLanguage(lang);
}
</script>

<template>
  <el-menu class="el-menu-demo" mode="horizontal" :ellipsis="false" router>
    <el-menu-item index="/">
      <div class="flex items-center justify-center gap-2">
        <img src="/dati.svg" alt="dati" class="size-7 object-contain" />
        <span class="text-base font-semibold leading-none">
          {{ t("header.brand") }}
        </span>
      </div>
    </el-menu-item>

    <el-sub-menu index="2">
      <template #title> {{ t("header.workspace") }} </template>
      <el-menu-item index="2-1"> {{ t("header.itemOne") }} </el-menu-item>
      <el-menu-item index="2-2"> {{ t("header.itemTwo") }} </el-menu-item>
    </el-sub-menu>

    <el-menu-item class="h-full" @click="toggleDark()">
      <button
        class="w-full cursor-pointer border-0 bg-transparent"
        style="height: var(--ep-menu-item-height)"
        :title="t('header.theme')"
      >
        <el-icon class="inline-flex">
          <component :is="isDark ? Moon : Sunny" />
        </el-icon>
      </button>
    </el-menu-item>

    <el-menu-item class="h-full">
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

    <el-menu-item class="h-full">
      <div class="size-full flex items-center justify-center">
        <el-avatar :size="24"> Z </el-avatar>
      </div>
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
