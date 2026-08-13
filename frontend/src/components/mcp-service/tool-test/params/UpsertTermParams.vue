<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { Plus, Delete } from "@element-plus/icons-vue";
import { stripEmpty } from "~/utils/stripEmpty";
import { getTermsBySubject } from "~/api/subject";
import type { TermVO } from "~/api/subject";

const props = defineProps<{ subjects: { id: string; name: string }[] }>();

const { t } = useI18n();

interface TermRow {
  subject_name: string;
  name: string;
  description: string;
  aliases: string[];
  subject_id?: string; // internal: platform subject id for term lookup (not sent)
}

const form = reactive<{ terms: TermRow[] }>({
  terms: [{ subject_name: "", name: "", description: "", aliases: [] }],
});

const canAddRow = computed(() => {
  const last = form.terms[form.terms.length - 1];
  return !!last && !!last.subject_name && !!last.name;
});

const addRow = () => {
  if (!canAddRow.value) return;
  form.terms.push({ subject_name: "", name: "", description: "", aliases: [] });
};

/** Remove a row; the last one stays disabled so at least one row remains. */
const removeRow = (i: number) => {
  if (form.terms.length > 1) {
    form.terms.splice(i, 1);
  }
};

const subjectIdByName = (name: string) =>
  props.subjects.find((s) => s.name === name)?.id ?? "";

// Term options per subject (platform API list carries current description/aliases)
const termsBySubject = ref<Record<string, TermVO[]>>({});
const termLoading = ref(false);

const ensureTermsLoaded = async (subjectId: string) => {
  if (!subjectId || termsBySubject.value[subjectId]) return;
  termLoading.value = true;
  try {
    const resp = await getTermsBySubject(subjectId, 1, 100);
    termsBySubject.value[subjectId] = resp.data || [];
  } catch {
    termsBySubject.value[subjectId] = [];
  } finally {
    termLoading.value = false;
  }
};

const termNamesFor = (entry: TermRow) =>
  (termsBySubject.value[entry.subject_id || ""] || []).map((tm) => tm.name);

/** Subject picked: load its terms, clear dependent fields. */
const onSubjectPicked = async (entry: TermRow) => {
  entry.name = "";
  entry.description = "";
  entry.aliases = [];
  entry.subject_id = subjectIdByName(entry.subject_name);
  await ensureTermsLoaded(entry.subject_id);
};

/**
 * Row mode: "update" = picked an existing term, "create" = typed a new name,
 * null = name not chosen yet.
 */
const modeOf = (entry: TermRow): "update" | "create" | null => {
  if (!entry.subject_id || !entry.name) return null;
  const exists = (termsBySubject.value[entry.subject_id] || []).some(
    (tm) => tm.name === entry.name,
  );
  return exists ? "update" : "create";
};

/**
 * Term picked. Two cases:
 * - selected an existing term → echo its current description/aliases (update flow)
 * - typed a brand-new name (allow-create) → fresh fields (create flow)
 */
const onTermPicked = (entry: TermRow) => {
  const term = (termsBySubject.value[entry.subject_id || ""] || []).find(
    (tm) => tm.name === entry.name,
  );
  entry.description = term?.description ?? "";
  entry.aliases = term?.aliases ? [...term.aliases] : [];
};

const descriptionPlaceholder = (entry: TermRow) =>
  modeOf(entry) === "update"
    ? t("mcpService.toolTest.descriptionPlaceholder")
    : t("common.placeholder.description");

defineExpose({
  getArgs: () => ({
    terms: form.terms.map((r) =>
      stripEmpty({
        subject_name: r.subject_name,
        name: r.name,
        description: r.description,
        aliases: r.aliases,
      }),
    ),
  }),
});
</script>

<template>
  <el-empty
    v-if="subjects.length === 0"
    :description="t('mcpService.toolTest.noSubjectInScope')"
    :image-size="60"
  />
  <div v-else class="flex flex-col gap-2.5">
    <div
      v-for="(entry, i) in form.terms"
      :key="i"
      class="flex flex-col gap-2 border border-[var(--ep-border-color-lighter)] rounded-lg p-2.5 bg-[var(--ep-fill-color-blank)] transition-colors hover:border-[var(--ep-border-color)]"
    >
      <!-- Row header: title + delete button -->
      <div class="flex items-center justify-between">
        <span class="text-xs font-semibold text-[var(--ep-text-color-secondary)]">
          {{ t("mcpService.toolTest.termRow", { n: i + 1 }) }}
        </span>
        <el-tooltip :content="t('common.delete')" placement="top">
          <el-button
            size="small"
            text
            type="danger"
            circle
            :icon="Delete"
            :aria-label="t('common.delete')"
            @click="removeRow(i)"
            :disabled="form.terms.length <= 1"
          />
        </el-tooltip>
      </div>

      <!-- Subject select -->
      <el-select
        v-model="entry.subject_name"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.subjectPlaceholder')"
        filterable
        @change="onSubjectPicked(entry)"
      >
        <el-option
          v-for="s in subjects"
          :key="s.id"
          :label="s.name"
          :value="s.name"
        />
      </el-select>

      <!-- Term name select / input -->
      <el-select
        v-model="entry.name"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.termNamePlaceholder')"
        filterable
        allow-create
        default-first-option
        :disabled="!entry.subject_name"
        :loading="termLoading"
        @change="onTermPicked(entry)"
      >
        <el-option
          v-for="n in termNamesFor(entry)"
          :key="n"
          :label="n"
          :value="n"
        />
      </el-select>

      <!-- Description textarea -->
      <el-input
        v-model="entry.description"
        size="small"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        :placeholder="descriptionPlaceholder(entry)"
      />

      <!-- Aliases input-tag -->
      <el-input-tag
        v-model="entry.aliases"
        size="small"
        class="w-full"
        :placeholder="t('mcpService.toolTest.aliasesPlaceholder')"
      />
    </div>

    <el-button
      size="small"
      class="w-full !border-dashed"
      :icon="Plus"
      :disabled="!canAddRow"
      @click="addRow"
    >
      {{ t("mcpService.toolTest.addTerm") }}
    </el-button>
  </div>
</template>
