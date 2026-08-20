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

export type ToolTestData = SqlExecution | TableMetadata | SearchHit | MetadataUpdateData | TableListData;

// ── METADATA_UPDATE (UPDATE_TABLE_INFO / UPDATE_COLUMN_INFO / UPSERT_TERM) ──

export interface MetadataUpdateData {
  type: "METADATA_UPDATE";
  results: MetadataUpdateResult[];
}

export interface MetadataUpdateResult {
  entity_type: "TABLE" | "COLUMN" | "TERM";
  entity: string;
  success: boolean;
  change_type?: "CREATE" | "UPDATE";
  old?: { description?: string; aliases?: string[] };
  new?: { description?: string; aliases?: string[] };
  error?: { error_category: string; message: string };
}

export interface SqlExecution {
  type: "SQL_EXECUTION";
  executed_sql: string;
  bindings?: any[];
  results: StatementResult[];
}

export interface TableMetadata {
  type: "TABLE_METADATA";
  tables: TableDef[];
}

export interface SearchHit {
  type: "SEARCH_HIT";
  keywords: string[];
  data_sources: DataSourceDef[];
  terms: TermDef[];
}

export interface TableListData {
  type: "TABLE_LIST";
  data_sources: DataSourceDef[];
}

export interface DataSourceDef {
  id: string;
  name: string;
  db_type?: string;
  default_schema?: string;
  description?: string;
  tables: TableDef[];
}

export interface TermDef {
  name: string;
  description: string;
  subject_name: string;
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

export interface TableDef {
  table: string;
  schema?: string;
  description?: string;
  aliases?: string[];
  columns?: ColumnDef[];
}

export interface ColumnDef {
  name: string;
  type: string;
  comment: string;
  aliases?: string[];
  sample_values?: string[];
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
