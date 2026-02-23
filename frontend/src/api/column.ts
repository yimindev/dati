import {get} from "~/api/http.ts";

export type TableColumnVO = {
  name: string;
  display_name?: string;
  data_type?: string;
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
  // 说明：
  // 1) URL 需要按你们实际后端路由调整
  // 2) schema 如果后端需要，可用 query 传递；不需要就删掉 params
  return get<{ data: TableColumnVO[] }>(
    `/v1/data-sources/${datasourceId}/tables/${tableId}/columns`,
    { page, size, keyword },
  );
}