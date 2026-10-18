import type { DataScopeAccessMode, DataScopeItem } from "~/api/mcp-service";

/** Effective access mode: SUBJECT scopes and legacy items fall back to IMPORTED_TABLES. */
export function accessModeOf(item: DataScopeItem): DataScopeAccessMode {
  return item.scope_type === "DATA_SOURCE" && item.access_mode === "MANAGED_SCHEMAS"
    ? "MANAGED_SCHEMAS"
    : "IMPORTED_TABLES";
}

export function isManagedSchemaMode(item: DataScopeItem): boolean {
  return accessModeOf(item) === "MANAGED_SCHEMAS";
}

/** Trim, drop blanks, deduplicate, and preserve input order. */
export function normalizeAllowedSchemas(schemas?: string[] | null): string[] {
  const result: string[] = [];
  const seen = new Set<string>();

  for (const raw of schemas ?? []) {
    const schema = raw?.trim();
    if (!schema || seen.has(schema)) continue;
    seen.add(schema);
    result.push(schema);
  }

  return result;
}

/** Schemas that are actually effective for this scope item. */
export function allowedSchemasOf(item: DataScopeItem): string[] {
  return isManagedSchemaMode(item) ? normalizeAllowedSchemas(item.allowed_schemas) : [];
}
