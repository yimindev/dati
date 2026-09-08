import type { PageResponse } from "~/api/types.ts";
import { get } from "./http";

export interface DailyStatItem {
  date: string;
  total_calls: number;
  success_calls: number;
  failed_calls: number;
  success_rate: number;
  avg_duration_ms: number;
}

export interface ToolStatItem {
  tool_name: string;
  call_count: number;
  percentage: number;
  success_rate: number;
}

export interface McpUsageStatsVO {
  total_calls: number;
  today_calls: number;
  success_rate: number;
  avg_duration_ms: number;
  daily_trend: DailyStatItem[];
  tool_distribution: ToolStatItem[];
}

export interface McpInvocationLogVO {
  id: string;
  service_id: string;
  method: string;
  tool_name?: string;
  caller_id?: string;
  caller_name?: string;
  client_ip?: string;
  success: boolean;
  duration_ms: number;
  error_message?: string;
  stat_date: string;
  created_at: string;
}

export function getMcpUsageStats(
  serviceId: string,
  days: number = 15,
  signal?: AbortSignal,
): Promise<McpUsageStatsVO> {
  return get<McpUsageStatsVO>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/stats`,
    { days },
    signal,
  );
}

export function listMcpInvocationLogs(
  serviceId: string,
  page: number = 1,
  size: number = 10,
  toolName?: string,
  success?: boolean,
  signal?: AbortSignal,
): Promise<PageResponse<McpInvocationLogVO>> {
  return get<PageResponse<McpInvocationLogVO>>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/invocation-logs`,
    {
      page,
      size,
      tool_name: toolName || undefined,
      success: success === undefined ? undefined : success,
    },
    signal,
  );
}
