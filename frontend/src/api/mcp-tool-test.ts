import { post } from "./http";

export interface ToolTestRequest {
  arguments: Record<string, any>;
}

export interface ToolTestResponse {
  success: boolean;
  execution_time_ms: number;
  data?: ToolTestData;
  error?: ToolTestError;
}

export interface ToolTestError {
  error_category: "PARAM_ERROR" | "SCOPE_ERROR" | "PERMISSION_DENIED" | "SQL_ERROR" | "TIMEOUT";
  message: string;
}

export type ToolTestData = SqlExecution | TableMetadata | SearchHit;

export interface SqlExecution {
  type: "SQL_EXECUTION";
  executed_sql: string;
  bindings?: any[];
  results: StatementResult[];
}

export interface TableMetadata {
  type: "TABLE_METADATA";
  tables: TableEntry[];
}

export interface SearchHit {
  type: "SEARCH_HIT";
}

export type StatementResult = SelectResult | WriteResult;

export interface SelectResult {
  success: true;
  type: "SELECT";
  columns: string[];
  rows: any[][];
  total_rows: number;
}

export interface WriteResult {
  success: boolean;
  type: "WRITE";
  affected_rows?: number;
  error_message?: string;
}

export interface TableEntry {
  success: boolean;
  table: string;
  schema?: string;
  columns?: ColumnDef[];
  error_message?: string;
}

export interface ColumnDef {
  name: string;
  type: string;
  comment: string;
}

export function testTool(
  serviceId: string,
  toolId: string,
  body: ToolTestRequest,
  signal?: AbortSignal,
): Promise<ToolTestResponse> {
  return post<ToolTestResponse, ToolTestRequest>(
    `/v1/mcp-services/${encodeURIComponent(serviceId)}/tools/${encodeURIComponent(toolId)}/test`,
    body,
    signal,
  );
}
