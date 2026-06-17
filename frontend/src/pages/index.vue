<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

interface Feature {
  key: string;
  path: string;
  label: string;
  description: string;
}

const features: Feature[] = [
  {
    key: "mcpBuilder",
    path: "/mcp-services",
    label: "mcpService.title",
    description: "mcpService.listSubtitle",
  },
  {
    key: "subjects",
    path: "/subjects",
    label: "subject.title",
    description: "subject.subtitle",
  },
  {
    key: "dataSources",
    path: "/datasources",
    label: "layout.side.dataSources",
    description: "datasource.subtitle",
  },
];

const featureItems = computed(() =>
  features.map((f) => ({
    key: f.key,
    path: f.path,
    labelText: t(f.label),
    descText: t(f.description),
  }))
);
</script>

<template>
  <div class="h-full flex flex-col items-center justify-center p-5 md:p-6">
    <!-- Logo & Brand -->
    <div class="text-center mb-12">
      <img
        src="/dati.svg"
        alt="DatI"
        loading="lazy"
        class="w-20 h-20 mx-auto mb-6 object-contain"
      />
      <h1 class="text-[20px] font-[650] mb-2">{{ t("layout.header.brand") }}</h1>
      <p class="text-[var(--ep-text-color-regular)]">
        {{ t("home.subtitle") }}
      </p>
    </div>

    <!-- Feature Cards -->
    <div class="flex flex-wrap justify-center gap-6">
      <RouterLink
        v-for="item in featureItems"
        :key="item.key"
        :to="item.path"
        :aria-label="item.labelText"
      >
        <el-card
          shadow="hover"
          class="w-48 h-40 cursor-pointer transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_4px_20px_var(--ep-color-primary-light-5)]"
        >
          <div class="flex flex-col items-center justify-center h-full">

            <span class="text-base font-medium mb-1">{{ item.labelText }}</span>
            <p class="text-xs text-[var(--ep-text-color-secondary)]">
              {{ item.descText }}
            </p>
          </div>
        </el-card>
      </RouterLink>
    </div>
  </div>
</template>
