import type { PageResponse } from "~/api/types.ts";
import { get, put, post } from "~/api/http.ts";

export type TableColumnVO = {
  id?: string;
  name: string;
  column_type?: string;
  nullable?: boolean;
  primary_key?: boolean;
  default_value?: string | null;
  description?: string;
  aliases?: string[];
  ordinal_position?: number;
  extract_value_enabled?: boolean;
};

export type ColumnValueVO = {
  id: string;
  value: string;
  synonyms: string[];
  _editing?: boolean;
  _synonymInput?: string;
};

export async function listTableColumns(
  datasourceId: string | number,
  tableId: string,
  page: number,
  size: number,
  keyword?: string,
) {
  return get<PageResponse<TableColumnVO>>(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns`,
    { page, size, keyword },
  );
}

export async function saveColumnMetadata(
  datasourceId: string | number,
  tableId: string,
  column: TableColumnVO,
) {
  return put(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${column.id}`,
    column,
  );
}

export async function extractColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
  overwrite: boolean = false,
) {
  return post(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values/extract?overwrite=${overwrite}`,
    {},
  );
}

export async function getColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
  page: number,
  size: number,
  keyword?: string,
) {
  return get<PageResponse<ColumnValueVO>>(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values`,
    { page, size, keyword },
  );
}

export async function saveColumnValues(
  datasourceId: string | number,
  tableId: string,
  columnId: string,
  values: ColumnValueVO[],
  deletedIds: string[],
) {
  return put(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns/${columnId}/values`,
    {
      values: values.map((v) => ({
        id: v.id,
        value: v.value,
        synonyms: v.synonyms,
      })),
      deleted_ids: deletedIds,
    },
  );
}
