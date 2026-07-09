import type { IdResponse } from "./types.ts";
import { get, post, put, del } from "./http";

export interface SqlPolicy {
  allow_select: boolean;
  allow_insert: boolean;
  allow_update: boolean;
  allow_delete: boolean;
  allow_ddl: boolean;
  allow_multi: boolean;
  allow_metadata: boolean;
  allow_transaction: boolean;
  allow_set: boolean;
}

export interface ToolParameter {
  name: string;
  type: "String" | "Number" | "Boolean" | "DateTime" | "Array";
  required: boolean;
  default_value?: string;
  description?: string;
}

export interface ParamSqlConfig {
  data_source_id: string;
  sql_template: string;
  parameters: ToolParameter[];
  sql_policy: SqlPolicy;
  timeout: number;
  max_rows: number;
}

export interface McpToolVO {
  id: string;
  tool_type: "SEARCH_METADATA" | "GET_TABLE_INFO" | "EXECUTE_SQL" | "PARAMETERIZED_SQL";
  name: string;
  title?: string;
  description: string;
  enabled: boolean;
  config: Record<string, any> | null;
}

export interface ToolsResponse {
  prebuilt: McpToolVO[];
  custom: McpToolVO[];
}

export interface PrebuiltToolPayload {
  enabled: boolean;
  config?: string;
}

export interface CustomToolPayload {
  tool_type: string;
  name: string;
  title?: string;
  description?: string;
  enabled?: boolean;
  config?: string;   // JSON of ParamSqlConfig
}

export function listTools(serviceId: string, signal?: AbortSignal): Promise<ToolsResponse> {
  return get<ToolsResponse>(`/v1/mcp-services/${encodeURIComponent(serviceId)}/tools`, undefined, signal);
}

export function updateTool(
  serviceId: string,
  toolId: string,
  body: PrebuiltToolPayload | CustomToolPayload,
  signal?: AbortSignal,
): Promise<IdResponse> {
  return put<IdResponse, typeof body>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/tools/${encodeURIComponent(toolId)}`,
    body,
    signal,
  );
}

export function createCustomTool(serviceId: string, body: CustomToolPayload, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, CustomToolPayload>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/tools`,
    body,
    signal,
  );
}

export function deleteCustomTool(serviceId: string, toolId: string, signal?: AbortSignal): Promise<IdResponse> {
  return del<IdResponse>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/tools/${encodeURIComponent(toolId)}`,
    undefined,
    signal,
  );
}
