import { describe, it, expect } from "vitest";
import zh from "../zh";
import en from "../en";

function flattenKeys(obj: Record<string, any>, prefix = ""): Record<string, string> {
  let result: Record<string, string> = {};
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof value === "object" && value !== null && !Array.isArray(value)) {
      Object.assign(result, flattenKeys(value, fullKey));
    } else {
      result[fullKey] = String(value);
    }
  }
  return result;
}

describe("i18n Locales Integrity", () => {
  const zhFlat = flattenKeys(zh);
  const enFlat = flattenKeys(en);

  it("should have identical key sets in zh and en locales", () => {
    const zhKeys = Object.keys(zhFlat).sort();
    const enKeys = Object.keys(enFlat).sort();

    const missingInEn = zhKeys.filter((k) => !(k in enFlat));
    const missingInZh = enKeys.filter((k) => !(k in zhFlat));

    expect(missingInEn, `Keys present in zh.ts but missing in en.ts: ${missingInEn.join(", ")}`).toEqual([]);
    expect(missingInZh, `Keys present in en.ts but missing in zh.ts: ${missingInZh.join(", ")}`).toEqual([]);
    expect(zhKeys).toEqual(enKeys);
  });

  it("should not contain empty translation values", () => {
    for (const [key, val] of Object.entries(zhFlat)) {
      expect(val.trim(), `zh.ts has empty value for key: ${key}`).not.toBe("");
    }
    for (const [key, val] of Object.entries(enFlat)) {
      expect(val.trim(), `en.ts has empty value for key: ${key}`).not.toBe("");
    }
  });

  it("should ensure all static t(...) keys used in source code exist in locale files", () => {
    // 使用 Vite 原生的 import.meta.glob 批量读取源码内容（无需依赖 node:fs / node:path，IDE 零告警）
    const sourceModules = import.meta.glob<string>(
      [
        "../../**/*.vue",
        "../../**/*.ts",
        "../../**/*.js",
        "!../../locales/**",
        "!../../**/*.test.ts",
        "!../../**/*.d.ts",
      ],
      { query: "?raw", import: "default", eager: true }
    );

    const staticKeyRegex = /(?:\$t|(?<!\w)t)\(\s*['"]([a-zA-Z0-9_.]+)['"]/g;
    const missingKeys: { file: string; key: string }[] = [];

    for (const [filePath, content] of Object.entries(sourceModules)) {
      let match: RegExpExecArray | null;
      while ((match = staticKeyRegex.exec(content)) !== null) {
        const key = match[1];
        if (!zhFlat[key]) {
          missingKeys.push({
            file: filePath,
            key,
          });
        }
      }
    }

    expect(
      missingKeys,
      `Static i18n keys called in code but missing from locale files: \n${JSON.stringify(missingKeys, null, 2)}`
    ).toEqual([]);
  });
});

