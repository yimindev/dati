import type { BaseResourceVO, IdResponse, PageResponse } from "~/api/types.ts";
import { get, post, put } from "./http";

export interface McpServiceVO extends BaseResourceVO {
  code: string;
  status: string;
  endpoint_path: string;
  tool_count: number;
}

export interface McpServicePayload {
  code?: string;
  name: string;
  description?: string;
}

export function createMcpService(body: McpServicePayload, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, McpServicePayload>("/v1/mcp-services", body, signal);
}

export function updateMcpService(id: string, body: McpServicePayload, signal?: AbortSignal): Promise<IdResponse> {
  return put<IdResponse, McpServicePayload>(`/v1/mcp-services/${encodeURIComponent(id)}`, body, signal);
}

export function getMcpService(id: string, signal?: AbortSignal): Promise<McpServiceVO> {
  return get<McpServiceVO>(`/v1/mcp-services/${encodeURIComponent(id)}`, undefined, signal);
}

export function listMcpServices(
  page: number,
  size: number,
  keyword?: string,
  status?: string,
  signal?: AbortSignal,
): Promise<PageResponse<McpServiceVO>> {
  return get<PageResponse<McpServiceVO>>("/v1/mcp-services", { page, size, keyword, status }, signal);
}

export interface DataScopeItem {
  id?: string;
  scope_type: "DATA_SOURCE" | "SUBJECT";
  reference_id: string;
  reference_name: string;
  table_names?: string[];
  tables?: Array<{
    name?: string;
    table_name?: string;
    schema?: string;
  }>;
}

export interface DataScopeResponse {
  items: DataScopeItem[];
}

export function getDataScope(id: string, signal?: AbortSignal): Promise<DataScopeResponse> {
  return get<DataScopeResponse>(`/v1/mcp-services/${encodeURIComponent(id)}/data-scope`, undefined, signal);
}

export function saveDataScope(
  id: string,
  body: { items: DataScopeItem[] },
  signal?: AbortSignal,
): Promise<IdResponse> {
  return put<IdResponse, { items: DataScopeItem[] }>(
    `/v1/mcp-services/${encodeURIComponent(id)}/data-scope`,
    body,
    signal,
  );
}
