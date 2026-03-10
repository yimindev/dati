import type { BaseResourceVO, IdResponse, PageResponse } from "~/api/types.ts";
import { get, post, del } from "./http";

export interface TableInfoVO extends BaseResourceVO {
  schema: string;
  datasource_id: string;
}

export function listTableInfos(datasourceId: string, page: number, size: number, keyword?: string){
  return get<PageResponse<TableInfoVO>>(
    "/v1/data-sources/" + datasourceId + "/tables",
    { page, size, keyword },
  );
}

export interface AddTableRequest {
  name: string;
  schema: string;
}

export function getAddedTableNames(datasourceId: string, signal?: AbortSignal): Promise<string[]> {
  return get<string[]>(
    `/v1/data-sources/${encodeURIComponent(datasourceId)}/tables/added-names`,
    undefined,
    signal
  );
}

export function batchAddTables(datasourceId: string, tables: AddTableRequest[], signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, AddTableRequest[]>(
    `/v1/data-sources/${encodeURIComponent(datasourceId)}/tables/batch`,
    tables,
    signal
  );
}

export function syncColumns(datasourceId: string, tableId: string, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, null>(
    `/v1/data-sources/${encodeURIComponent(datasourceId)}/tables/${encodeURIComponent(tableId)}/columns/sync`,
    null,
    signal
  );
}

export function deleteTable(datasourceId: string, tableId: string, signal?: AbortSignal): Promise<void> {
  return del<void>(
    `/v1/data-sources/${encodeURIComponent(datasourceId)}/tables/${encodeURIComponent(tableId)}`,
    undefined,
    signal
  );
}
