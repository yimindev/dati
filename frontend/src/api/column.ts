import type { PageResponse } from "~/api/types.ts";
import {get} from "~/api/http.ts";

export type TableColumnVO = {
  name: string;
  column_type?: string;
  nullable?: boolean;
  primary_key?: boolean;
  default_value?: string | null;
  description?: string;
  ordinal_position?: number;
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