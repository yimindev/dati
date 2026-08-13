import { ref } from "vue";
import { listTableInfos } from "~/api/tableinfo";
import { listTableColumns } from "~/api/column";
import type { TableColumnVO } from "~/api/column";

/** A platform table option with metadata used to echo current values. */
export interface TableInfoEntry {
  id: string;
  schema: string;
  name: string;
  description: string;
  aliases: string[];
}

/**
 * Module-level caches shared by all table-picker forms (tool-test params).
 * Reset by the dialog shell on each open so stale lists are refetched.
 */
const tablesByDs = ref<Record<string, TableInfoEntry[]>>({});
const tableLoading = ref(false);
const columnsByTable = ref<Record<string, TableColumnVO[]>>({});
const columnLoading = ref(false);

/** Clear cached table/column lists (call when the test dialog opens). */
export function resetTablePickerCache() {
  tablesByDs.value = {};
  columnsByTable.value = {};
}

export function useTablePicker() {
  const ensureTablesLoaded = async (dsId: string) => {
    if (!dsId || tablesByDs.value[dsId]) return;
    tableLoading.value = true;
    try {
      const resp = await listTableInfos(dsId, 1, 100);
      tablesByDs.value[dsId] = (resp.data || []).map((t) => ({
        id: t.id,
        schema: t.schema,
        name: t.name,
        description: t.description ?? "",
        aliases: t.aliases ?? [],
      }));
    } catch {
      tablesByDs.value[dsId] = [];
    } finally {
      tableLoading.value = false;
    }
  };

  const schemaOptionsFor = (entry: any) =>
    [
      ...new Set(
        (tablesByDs.value[entry?.data_source_id] || [])
          .map((t) => t.schema)
          .filter(Boolean),
      ),
    ].sort() as string[];

  const tableOptionsFor = (entry: any) =>
    (tablesByDs.value[entry?.data_source_id] || [])
      .filter((t) => !entry.schema || t.schema === entry.schema)
      .map((t) => ({ name: t.name }));

  const colKey = (entry: any) =>
    `${entry?.data_source_id ?? ""}\u0000${entry?.schema ?? ""}\u0000${entry?.table ?? ""}`;

  const ensureColumnsLoaded = async (entry: any) => {
    const key = colKey(entry);
    if (!entry?.data_source_id || !entry?.table || columnsByTable.value[key])
      return;
    const table = tablesByDs.value[entry.data_source_id]?.find(
      (t) => t.name === entry.table && (!entry.schema || t.schema === entry.schema),
    );
    if (!table) return;
    columnLoading.value = true;
    try {
      const resp = await listTableColumns(entry.data_source_id, table.id, 1, 100);
      columnsByTable.value[key] = resp.data || [];
    } catch {
      columnsByTable.value[key] = [];
    } finally {
      columnLoading.value = false;
    }
  };

  const columnOptionsFor = (entry: any) =>
    (columnsByTable.value[colKey(entry)] || []).map((c) => c.name);

  const findTableInDs = (entry: any) =>
    tablesByDs.value[entry?.data_source_id]?.find(
      (t) => t.name === entry.table && (!entry.schema || t.schema === entry.schema),
    );

  /** Data source picked: reset dependent fields. */
  const onDsPicked = (entry: any) => {
    entry.schema = "";
    entry.table = "";
    entry.column = "";
    entry.description = "";
    entry.aliases = [];
  };

  /** Table picked: refresh and echo current table description/aliases, then load its columns. */
  const onTablePicked = async (entry: any) => {
    entry.column = "";
    entry.description = "";
    entry.aliases = [];
    const table = findTableInDs(entry);
    if (!table) return;
    // Refresh current values — the open-dialog cache may be stale (other clients may have updated)
    try {
      const resp = await listTableInfos(entry.data_source_id, 1, 100, table.name);
      const fresh = (resp.data || []).find(
        (t) => t.name === table.name && (!entry.schema || t.schema === entry.schema),
      );
      if (fresh) {
        entry.description = fresh.description ?? "";
        entry.aliases = fresh.aliases ? [...fresh.aliases] : [];
      }
    } catch {
      /* keep empty values on failure */
    }
    ensureColumnsLoaded(entry);
  };

  /** Table picked in column metadata form: clear column & metadata, load columns (do NOT echo table metadata). */
  const onTablePickedForColumn = (entry: any) => {
    entry.column = "";
    entry.description = "";
    entry.aliases = [];
    ensureColumnsLoaded(entry);
  };

  /** Column picked: echo current column description/aliases. */
  const onColumnPicked = (entry: any) => {
    const col = (columnsByTable.value[colKey(entry)] || []).find(
      (c) => c.name === entry.column,
    );
    entry.description = col?.description ?? "";
    entry.aliases = col?.aliases ? [...col.aliases] : [];
  };

  return {
    tablesByDs,
    tableLoading,
    columnsByTable,
    columnLoading,
    ensureTablesLoaded,
    schemaOptionsFor,
    tableOptionsFor,
    ensureColumnsLoaded,
    columnOptionsFor,
    findTableInDs,
    onDsPicked,
    onTablePicked,
    onTablePickedForColumn,
    onColumnPicked,
  };
}
