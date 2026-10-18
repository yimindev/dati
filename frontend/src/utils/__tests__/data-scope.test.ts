import { describe, expect, it } from "vitest";
import {
  accessModeOf,
  allowedSchemasOf,
  isManagedSchemaMode,
  normalizeAllowedSchemas,
} from "../data-scope";
import type { DataScopeItem } from "~/api/mcp-service";

function dataSource(overrides: Partial<DataScopeItem> = {}): DataScopeItem {
  return {
    scope_type: "DATA_SOURCE",
    reference_id: "ds-1",
    ...overrides,
  };
}

describe("data scope access mode", () => {
  it("defaults data sources to IMPORTED_TABLES", () => {
    const item = dataSource();

    expect(accessModeOf(item)).toBe("IMPORTED_TABLES");
    expect(isManagedSchemaMode(item)).toBe(false);
    expect(allowedSchemasOf(item)).toEqual([]);
  });

  it("keeps MANAGED_SCHEMAS and normalizes allowed schemas", () => {
    const item = dataSource({
      access_mode: "MANAGED_SCHEMAS",
      allowed_schemas: [" sandbox ", "public", "sandbox", ""],
    });

    expect(accessModeOf(item)).toBe("MANAGED_SCHEMAS");
    expect(isManagedSchemaMode(item)).toBe(true);
    expect(allowedSchemasOf(item)).toEqual(["sandbox", "public"]);
  });

  it("ignores access mode on subject scopes", () => {
    const item: DataScopeItem = {
      scope_type: "SUBJECT",
      reference_id: "subject-1",
      access_mode: "MANAGED_SCHEMAS",
      allowed_schemas: ["sandbox"],
    };

    expect(accessModeOf(item)).toBe("IMPORTED_TABLES");
    expect(isManagedSchemaMode(item)).toBe(false);
    expect(allowedSchemasOf(item)).toEqual([]);
  });

  it("normalizes null and undefined schema lists", () => {
    expect(normalizeAllowedSchemas()).toEqual([]);
    expect(normalizeAllowedSchemas(null)).toEqual([]);
    expect(normalizeAllowedSchemas(["", "  "])).toEqual([]);
  });

  it("preserves first-seen order while deduplicating", () => {
    expect(normalizeAllowedSchemas([" zeta ", "alpha", "zeta", "beta"])).toEqual([
      "zeta",
      "alpha",
      "beta",
    ]);
  });
});
