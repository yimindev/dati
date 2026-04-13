import type { BaseResourceVO, IdResponse, PageResponse } from "~/api/types.ts";
import { get, post, put, del } from "./http";


// 兼容 DataSource：创建/编辑时我们直接发 JSON
export interface DatasourceVO extends BaseResourceVO {
  jdbc_url: string;
  username: string;
  type: string;
}

// 创建/更新的请求体（蛇形命名）
export interface DataSourcePayload {
  name?: string;
  description?: string;
  jdbc_url: string;
  username: string;
  password?: string;
  type: string;
}

// 列信息（常用字段集合，按蛇形命名）
export interface ColumnVO {
  name: string;
  type_name?: string;
  data_type?: number;
  column_size?: number;
  nullable?: boolean;
  remarks?: string;
  ordinal_position?: number;
  is_primary_key?: boolean;
  is_auto_increment?: boolean;
}

// 表信息
export interface TableVO {
  name: string;
  comment?: string;
}

// 执行 SQL 的请求体
export interface SqlExecuteRequest {
  sql: string;
}

// 执行 SQL 返回的每一行是动态列集合，这里使用索引签名（避免 Record/any）
export type SqlRow = { [column: string]: unknown };

// 查询 catalog 的可选参数
export interface CatalogParam {
  catalog?: string;
}

// 数据源：测试连接（POST /v1/data-sources/test-connection）
// 注意：请求体字段用蛇形（如 jdbc_url、username、password、type）
export function testConnection(body: DataSourcePayload, signal?: AbortSignal): Promise<boolean> {
  return post<boolean, DataSourcePayload>('/v1/data-sources/test-connection', body, signal)
}

// 数据源：新增（POST /v1/data-sources）
export function addDataSource(body: DataSourcePayload, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, DataSourcePayload>('/v1/data-sources', body, signal)
}

// 数据源：更新（PUT /v1/data-sources/{id}）
export function updateDataSource(id: string, body: DataSourcePayload, signal?: AbortSignal): Promise<IdResponse> {
  return put<IdResponse, DataSourcePayload>(`/v1/data-sources/${encodeURIComponent(id)}`, body, signal)
}

// 数据源：删除（DELETE /v1/data-sources/{id}）
export function deleteDataSource(id: string, signal?: AbortSignal): Promise<IdResponse> {
  return del<IdResponse>(`/v1/data-sources/${encodeURIComponent(id)}`, undefined, signal)
}

// 数据源：列表（GET /v1/data-sources）
export function listDataSources(page: number, size: number, keyword?: string, signal?: AbortSignal): Promise<PageResponse<DatasourceVO>> {
  return get<PageResponse<DatasourceVO>>(
    "/v1/data-sources",
    { page, size, keyword },
    signal,
  );
}

// Schema 列表（GET /v1/data-sources/{id}/schemas?catalog=...）
export function getSchemas(id: string, params?: CatalogParam, signal?: AbortSignal): Promise<string[]> {
  return get<string[]>(`/v1/data-sources/${encodeURIComponent(id)}/schemas`, params, signal)
}

// 表列表（GET /v1/data-sources/{id}/schemas/{schema}/tables?catalog=...）
export function getTables(
  id: string,
  schema: string,
  params?: CatalogParam,
  signal?: AbortSignal
): Promise<TableVO[]> {
  return get<TableVO[]>(
    `/v1/data-sources/${encodeURIComponent(id)}/schemas/${encodeURIComponent(schema)}/tables`,
    params,
    signal
  )
}

// 列列表（GET /v1/data-sources/{id}/schemas/{schema}/tables/{table}/columns?catalog=...）
export function getColumns(
  id: string,
  schema: string,
  table: string,
  params?: CatalogParam,
  signal?: AbortSignal
): Promise<ColumnVO[]> {
  return get<ColumnVO[]>(
    `/v1/data-sources/${encodeURIComponent(id)}/schemas/${encodeURIComponent(schema)}/tables/${encodeURIComponent(table)}/columns`,
    params,
    signal
  )
}

// 执行 SQL（POST /v1/data-sources/{id}/execute-sql）
export function executeSql(
  id: string,
  body: SqlExecuteRequest,
  signal?: AbortSignal
): Promise<SqlRow[]> {
  return post<SqlRow[], SqlExecuteRequest>(
    `/v1/data-sources/${encodeURIComponent(id)}/execute-sql`,
    body,
    signal
  )
}