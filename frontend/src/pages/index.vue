<script setup lang="ts">
import { useI18n } from "vue-i18n";
import {
  Collection,
  DataLine,
  Connection,
} from "@element-plus/icons-vue";

const { t } = useI18n();

const features = [
  {
    key: "mcpBuilder",
    icon: Collection,
    path: "/mcp-services",
    label: "mcpService.title",
  },
  {
    key: "semanticModels",
    icon: DataLine,
    path: "/nav/2",
  },
  {
    key: "dataSources",
    icon: Connection,
    path: "/datasources",
  },
];
</script>

<template>
  <div class="h-full flex flex-col items-center justify-center p-8">
    <!-- Logo & Brand -->
    <div class="text-center mb-12">
      <img
        src="/dati.svg"
        alt="DatI"
        class="w-20 h-20 mx-auto mb-6 object-contain"
      />
      <h1 class="text-[20px] font-[650] mb-2">DatI</h1>
      <p class="text-[var(--ep-text-color-regular)]">
        {{ t("home.subtitle") }}
      </p>
    </div>

    <!-- Feature Cards -->
    <div class="flex flex-wrap justify-center gap-6">
      <RouterLink
        v-for="feature in features"
        :key="feature.key"
        :to="feature.path"
        class="group"
      >
        <el-card
          shadow="hover"
          class="w-48 h-36 flex flex-col items-center justify-center cursor-pointer transition-all duration-300 hover:shadow-lg hover:-translate-y-1"
        >
          <el-icon class="text-4xl mb-4 text-[var(--ep-color-primary)]">
            <component :is="feature.icon" />
          </el-icon>
          <span class="text-base font-medium">
            {{ feature.label ? t(feature.label) : t(`layout.side.${feature.key}`) }}
          </span>
        </el-card>
      </RouterLink>
    </div>
  </div>
</template>
