<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import {
  Menu as IconMenu,
  Right,
  Reading,
  Lock,
  RefreshRight,
  Connection,
} from "@element-plus/icons-vue";
import { listDataSources } from "~/api/datasource";
import { listSubjects } from "~/api/subject";
import { listMcpServices } from "~/api/mcp-service";

const { t } = useI18n();
const router = useRouter();

// State
const loadingStats = ref(true);
const datasourceCount = ref(0);
const subjectCount = ref(0);
const mcpServiceCount = ref(0);

// Load data overview
async function fetchOverview() {
  loadingStats.value = true;
  try {
    const [dsRes, subRes, mcpRes] = await Promise.allSettled([
      listDataSources(1, 1),
      listSubjects(1, 1),
      listMcpServices(1, 1),
    ]);

    if (dsRes.status === "fulfilled") {
      datasourceCount.value = dsRes.value.total ?? 0;
    }
    if (subRes.status === "fulfilled") {
      subjectCount.value = subRes.value.total ?? 0;
    }
    if (mcpRes.status === "fulfilled") {
      mcpServiceCount.value = mcpRes.value.total ?? 0;
    }
  } catch {
    // Silent fail for stats on home
  } finally {
    loadingStats.value = false;
  }
}

// Workflow step definition
const workflowSteps = computed(() => [
  {
    id: "datasource",
    iconClass: "icon-[codicon--database]",
    titleKey: "home.workflow.step1Title",
    descKey: "home.workflow.step1Desc",
    statLabelKey: "home.stats.dataSources",
    countText: t("home.workflow.countUnit", { count: datasourceCount.value }),
    path: "/datasources",
  },
  {
    id: "subject",
    iconComponent: IconMenu,
    titleKey: "home.workflow.step2Title",
    descKey: "home.workflow.step2Desc",
    statLabelKey: "home.stats.subjects",
    countText: t("home.workflow.countUnit", { count: subjectCount.value }),
    path: "/subjects",
  },
  {
    id: "mcp",
    iconClass: "icon-[codicon--mcp]",
    titleKey: "home.workflow.step3Title",
    descKey: "home.workflow.step3Desc",
    statLabelKey: "home.stats.mcpServices",
    countText: t("home.workflow.countUnit", { count: mcpServiceCount.value }),
    path: "/mcp-services",
  },
]);

function go(path: string) {
  router.push(path);
}

onMounted(() => {
  fetchOverview();
});
</script>

<template>
  <div class="h-full max-w-6xl xl:max-w-7xl 2xl:max-w-[1440px] mx-auto px-6 sm:px-8 lg:px-12 py-6 flex flex-col justify-evenly gap-6">
    <!-- 1. Hero Section -->
    <div class="flex flex-col items-center text-center space-y-3.5 max-w-2xl xl:max-w-3xl mx-auto">
      <!-- Brand Pill Badge -->
      <div class="inline-flex items-center gap-2 px-3.5 py-1 rounded-full border border-[var(--ep-border-color-lighter)] bg-[var(--ep-bg-color)] text-xs lg:text-sm text-[var(--ep-text-color-secondary)]">
        <img
          src="/dati.svg"
          alt="DatI"
          class="w-4 h-4 object-contain"
        />
        <span class="font-semibold text-[var(--ep-text-color-primary)]">DatI</span>
        <span class="w-1.5 h-1.5 rounded-full bg-[var(--ep-color-primary)]"></span>
        <span>{{ t("home.brandTagline") }}</span>
      </div>

      <!-- Main Headline -->
      <h1 class="text-2xl sm:text-3xl lg:text-4xl font-bold tracking-tight text-[var(--ep-text-color-primary)] leading-tight">
        {{ t("home.heroTitle") }}
      </h1>

      <!-- Subtitle -->
      <p class="text-xs sm:text-sm lg:text-base text-[var(--ep-text-color-regular)] leading-relaxed max-w-xl xl:max-w-2xl">
        {{ t("home.heroDesc") }}
      </p>

      <!-- Quiet docs link -->
      <a
        href="/docs/"
        target="_blank"
        rel="noopener"
        class="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full border border-[var(--ep-border-color-lighter)] bg-[var(--ep-fill-color-light)] text-xs lg:text-sm text-[var(--ep-color-primary)] hover:border-[var(--ep-color-primary-light-5)] hover:bg-[var(--ep-color-primary-light-9)] transition-all"
      >
        <el-icon><Reading /></el-icon>
        <span>{{ t("home.docLink") }}</span>
        <el-icon :size="12"><Right /></el-icon>
      </a>
    </div>

    <!-- 2. Core 3-Step Workflow Deck -->
    <div class="space-y-4">
      <!-- Section Header -->
      <div class="text-center">
        <h2 class="text-base lg:text-lg font-bold text-[var(--ep-text-color-primary)] tracking-tight">
          {{ t("home.workflow.title") }}
        </h2>
      </div>

      <!-- 3 Interactive Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-5 lg:gap-6 2xl:gap-8">
        <div
          v-for="step in workflowSteps"
          :key="step.id"
          role="link"
          tabindex="0"
          @click="go(step.path)"
          @keydown.enter.prevent="go(step.path)"
          class="group h-full p-5 md:p-6 lg:p-7 rounded-xl border border-[var(--ep-border-color-lighter)] bg-[var(--ep-bg-color)] flex flex-col justify-between gap-4 hover:border-[var(--ep-color-primary-light-5)] hover:shadow-md transition-all cursor-pointer"
        >
          <div class="space-y-2.5">
            <!-- Icon + Title (entry point) -->
            <div class="flex items-center gap-2.5">
              <div class="w-8 h-8 lg:w-9 lg:h-9 shrink-0 rounded-lg flex items-center justify-center bg-[var(--ep-color-primary-light-9)] text-[var(--ep-color-primary)]">
                <span v-if="step.iconClass" :class="[step.iconClass, 'text-base lg:text-lg']"></span>
                <el-icon v-else-if="step.iconComponent" :size="16">
                  <component :is="step.iconComponent" />
                </el-icon>
              </div>
              <h3 class="text-sm lg:text-base font-semibold text-[var(--ep-text-color-primary)] flex items-center gap-1 group-hover:text-[var(--ep-color-primary)] transition-colors">
                <span>{{ t(step.titleKey) }}</span>
                <el-icon :size="14" class="transition-transform group-hover:translate-x-1"><Right /></el-icon>
              </h3>
            </div>

            <!-- Desc (fixed 2-line height for uniform layout) -->
            <p class="text-xs lg:text-sm text-[var(--ep-text-color-regular)] leading-relaxed line-clamp-2 min-h-[36px] lg:min-h-[40px]">
              {{ t(step.descKey) }}
            </p>
          </div>

          <!-- Metric Capsule (strictly aligned at bottom) -->
          <div class="p-2.5 rounded-md bg-[var(--ep-fill-color-light)] flex items-center justify-between text-xs lg:text-sm">
            <span class="text-[var(--ep-text-color-secondary)]">{{ t(step.statLabelKey) }}</span>
            <el-skeleton v-if="loadingStats" animated class="w-14">
              <template #template>
                <el-skeleton-item variant="text" style="width: 36px" />
              </template>
            </el-skeleton>
            <span v-else class="font-medium text-[var(--ep-text-color-primary)] truncate max-w-[150px]">
              {{ step.countText }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Bottom Ecosystem Highlights -->
    <div class="flex flex-wrap items-center justify-center gap-6 md:gap-10 text-xs lg:text-sm text-[var(--ep-text-color-secondary)] pt-3 border-t border-[var(--ep-border-color-lighter)]">
      <el-tooltip :content="t('home.features.standardProtocolDesc')" placement="top">
        <div class="flex items-center gap-2 cursor-help hover:text-[var(--ep-text-color-primary)] transition-colors">
          <el-icon class="text-[var(--ep-color-primary)]"><Connection /></el-icon>
          <span>{{ t("home.features.standardProtocol") }} (2025-11-25)</span>
        </div>
      </el-tooltip>
      <el-tooltip :content="t('home.features.securityDesc')" placement="top">
        <div class="flex items-center gap-2 cursor-help hover:text-[var(--ep-text-color-primary)] transition-colors">
          <el-icon class="text-[var(--ep-color-primary)]"><Lock /></el-icon>
          <span>{{ t("home.features.security") }}</span>
        </div>
      </el-tooltip>
      <el-tooltip :content="t('home.features.ecosystemDesc')" placement="top">
        <div class="flex items-center gap-2 cursor-help hover:text-[var(--ep-text-color-primary)] transition-colors">
          <el-icon class="text-[var(--ep-color-primary)]"><RefreshRight /></el-icon>
          <span>{{ t("home.features.ecosystem") }}</span>
        </div>
      </el-tooltip>
    </div>

  </div>
</template>
