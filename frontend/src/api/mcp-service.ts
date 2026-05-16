import type { BaseResourceVO, IdResponse, PageResponse } from "~/api/types.ts";
import { get, post, put } from "./http";

export interface McpServiceVO extends BaseResourceVO {
  status: string;
  endpoint_path: string;
  tool_count: number;
}

export interface McpServicePayload {
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
