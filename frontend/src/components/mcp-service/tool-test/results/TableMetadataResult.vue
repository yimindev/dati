<script setup lang="ts">
import { CircleCheckFilled } from "@element-plus/icons-vue";
import type { TableMetadata } from "~/api/mcp-tool-test";

defineProps<{ data: TableMetadata }>();
</script>

<template>
  <template v-for="entry in data.tables" :key="entry.table">
    <div
      class="result-card border border-[var(--ep-border-color-lighter)] rounded-lg overflow-hidden mb-3"
    >
      <div
        class="result-card-header flex items-center gap-1.5 text-xs font-semibold text-[var(--ep-text-color-secondary)] bg-[var(--ep-fill-color)] py-2 px-3"
      >
        <el-icon class="text-[var(--ep-color-success)]"
          ><CircleCheckFilled
        /></el-icon>
        <span>{{ entry.schema ? entry.schema + "." : "" }}{{ entry.table }}</span>
      </div>
      <div class="result-card-body p-0">
        <el-table v-if="entry.columns" :data="entry.columns" border size="small">
          <el-table-column prop="name" label="Column" width="140" />
          <el-table-column prop="type" label="Type" width="120" />
          <el-table-column prop="comment" label="Comment" min-width="120" />
          <el-table-column label="Aliases" width="140">
            <template #default="{ row }">
              <el-tag
                v-for="a in row.aliases"
                :key="a"
                size="small"
                class="mr-1"
                >{{ a }}</el-tag
              >
            </template>
          </el-table-column>
          <el-table-column label="Sample Values" min-width="160">
            <template #default="{ row }">
              <el-tag
                v-for="v in row.sample_values"
                :key="v"
                size="small"
                type="info"
                class="mr-1"
                >{{ v }}</el-tag
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </template>
</template>
