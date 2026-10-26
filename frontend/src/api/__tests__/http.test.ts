import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("element-plus", () => {
  const message = vi.fn();
  return {
    ElMessage: Object.assign(message, {
      error: vi.fn(),
      success: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
    }),
  };
});

import { i18n } from "~/plugins/i18n";
import en from "~/locales/en";
import { ApiError } from "../error";
import {
  acceptLanguageHeader,
  http,
  isAuthEntryUrl,
  isHandledError,
} from "../http";

beforeEach(() => {
  i18n.global.setLocaleMessage("en", en);
  i18n.global.locale.value = "en";
  vi.stubGlobal("localStorage", {
    getItem: vi.fn(() => "token-123"),
    removeItem: vi.fn(),
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("http helpers", () => {
  it("maps the console locale to a standard Accept-Language value", () => {
    expect(acceptLanguageHeader("zh")).toBe("zh-CN");
    expect(acceptLanguageHeader("en")).toBe("en");
  });

  it("detects auth endpoints", () => {
    expect(isAuthEntryUrl("/v1/auth/login")).toBe(true);
    expect(isAuthEntryUrl("/v1/auth/register")).toBe(true);
    expect(isAuthEntryUrl("/v1/auth/guest")).toBe(true);
    expect(isAuthEntryUrl("/v1/auth/api-keys")).toBe(false);
    expect(isAuthEntryUrl("/v1/auth/me")).toBe(false);
    expect(isAuthEntryUrl("/v1/data-sources")).toBe(false);
    expect(isAuthEntryUrl(undefined)).toBe(false);
  });

  it("checks the handled flag on ApiError", () => {
    const error = new ApiError("http", 403, "No permission");
    expect(isHandledError(error)).toBe(false);
    error.handled = true;
    expect(isHandledError(error)).toBe(true);
    expect(isHandledError(null)).toBe(false);
  });
});

describe("http interceptors", () => {
  it("adds the bearer token, Accept-Language, and unwraps response data", async () => {
    let captured: any;
    const adapter = async (config: any) => {
      captured = config;
      return {
        data: { ok: true },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      };
    };

    i18n.global.locale.value = "zh";
    const result = await http.get("/ping", { adapter });

    expect(result).toEqual({ ok: true });
    expect(captured.headers.get("Authorization")).toBe("Bearer token-123");
    expect(captured.headers.get("Accept-Language")).toBe("zh-CN");
  });
});
