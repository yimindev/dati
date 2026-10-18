import { describe, expect, it } from "vitest";
import {
  accessModeOf,
  allowedSchemasOf,
  isSchemaScopeMode,
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
    expect(isSchemaScopeMode(item)).toBe(false);
    expect(allowedSchemasOf(item)).toEqual([]);
  });

  it("keeps SCHEMA_SCOPE and normalizes allowed schemas", () => {
    const item = dataSource({
      access_mode: "SCHEMA_SCOPE",
      allowed_schemas: [" sandbox ", "public", "sandbox", ""],
    });

    expect(accessModeOf(item)).toBe("SCHEMA_SCOPE");
    expect(isSchemaScopeMode(item)).toBe(true);
    expect(allowedSchemasOf(item)).toEqual(["sandbox", "public"]);
  });

  it("ignores access mode on subject scopes", () => {
    const item: DataScopeItem = {
      scope_type: "SUBJECT",
      reference_id: "subject-1",
      access_mode: "SCHEMA_SCOPE",
      allowed_schemas: ["sandbox"],
    };

    expect(accessModeOf(item)).toBe("IMPORTED_TABLES");
    expect(isSchemaScopeMode(item)).toBe(false);
    expect(allowedSchemasOf(item)).toEqual([]);
  });

  it("normalizes null and undefined schema lists", () => {
    expect(normalizeAllowedSchemas()).toEqual([]);
    expect(normalizeAllowedSchemas(null)).toEqual([]);
    expect(normalizeAllowedSchemas(["", "  "])).toEqual([]);
  });

  it("preserves first-seen order while deduplicating", () => {
    expect(
      normalizeAllowedSchemas([" zeta ", "alpha", "zeta", "beta"]),
    ).toEqual(["zeta", "alpha", "beta"]);
  });
});
