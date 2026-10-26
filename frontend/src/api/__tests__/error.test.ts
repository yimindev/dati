import { beforeEach, describe, expect, it, vi } from "vitest";

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

import { ElMessage } from "element-plus";
import en from "~/locales/en";
import { i18n } from "~/plugins/i18n";
import {
  ApiError,
  isHandledError,
  normalizeError,
  notifyError,
  resolveErrorMessage,
} from "../error";

const messageSpy = vi.mocked(ElMessage);

function axiosError(overrides: Record<string, any>): Record<string, any> {
  return {
    isAxiosError: true,
    message: "Request failed",
    config: {},
    ...overrides,
  };
}

beforeEach(() => {
  messageSpy.mockClear();
  i18n.global.setLocaleMessage("en", en);
  i18n.global.locale.value = "en";
});

describe("normalizeError", () => {
  it("keeps an existing ApiError unchanged", () => {
    const original = new ApiError("http", 409, "conflict", { code: "MS010" });
    expect(normalizeError(original)).toBe(original);
  });

  it("extracts the backend code and message", () => {
    const error = normalizeError(
      axiosError({
        response: {
          status: 409,
          data: { code: "MS010", message: "Service code already exists: demo" },
        },
      }),
    );

    expect(error.kind).toBe("http");
    expect(error.status).toBe(409);
    expect(error.code).toBe("MS010");
    expect(error.serverMessage).toBe("Service code already exists: demo");
  });

  it("ignores non-JSON error bodies", () => {
    const error = normalizeError(
      axiosError({
        message: "Request failed with status code 502",
        response: { status: 502, data: "<html>Bad Gateway</html>" },
      }),
    );

    expect(error.kind).toBe("http");
    expect(error.status).toBe(502);
    expect(error.code).toBeUndefined();
    expect(error.serverMessage).toBeUndefined();
  });

  it("detects network failures", () => {
    const error = normalizeError(axiosError({ message: "Network Error" }));
    expect(error.kind).toBe("network");
    expect(error.status).toBe(0);
  });

  it("detects timeouts", () => {
    const error = normalizeError(
      axiosError({ code: "ECONNABORTED", message: "timeout of 15000ms exceeded" }),
    );
    expect(error.kind).toBe("timeout");
  });

  it("detects cancelled requests", () => {
    const error = normalizeError(axiosError({ __CANCEL__: true, message: "canceled" }));
    expect(error.kind).toBe("canceled");
  });

  it("keeps ordinary Error messages", () => {
    const error = normalizeError(new Error("custom failure"));
    expect(error.kind).toBe("unknown");
    expect(error.message).toBe("custom failure");
  });
});

describe("resolveErrorMessage", () => {
  it("prefers the backend message for 4xx errors without appending the code", () => {
    const error = new ApiError("http", 409, "Service code already exists: demo", {
      code: "MS010",
      serverMessage: "Service code already exists: demo",
    });

    expect(resolveErrorMessage(error, "fallback")).toBe(
      "Service code already exists: demo",
    );
  });

  it("appends the code for 5xx errors", () => {
    const error = new ApiError("http", 500, "Internal server error", {
      code: "CM003",
      serverMessage: "Internal server error",
    });

    expect(resolveErrorMessage(error, "fallback")).toBe(
      "Internal server error (CM003)",
    );
  });

  it("uses a localized status fallback when the backend has no message", () => {
    const error = new ApiError("http", 502, "Request failed with status code 502");
    expect(resolveErrorMessage(error, "fallback")).toBe(
      "The service is temporarily unavailable. Please try again later.",
    );
  });

  it("uses localized network and timeout messages", () => {
    expect(resolveErrorMessage(new ApiError("network", 0, "Network Error"), "fallback")).toBe(
      "Network connection failed. Check your network and try again.",
    );
    expect(resolveErrorMessage(new ApiError("timeout", 0, "timeout"), "fallback")).toBe(
      "Request timed out. Please try again.",
    );
  });

  it("keeps ordinary Error messages and falls back when empty", () => {
    expect(resolveErrorMessage(new ApiError("unknown", 0, "custom failure"), "fallback")).toBe(
      "custom failure",
    );
    expect(resolveErrorMessage(new ApiError("unknown", 0, ""), "fallback")).toBe(
      "fallback",
    );
  });

  it("returns an empty string for cancelled requests", () => {
    expect(resolveErrorMessage(new ApiError("canceled", 0, "canceled"), "fallback")).toBe("");
  });
});

describe("notifyError", () => {
  it("ignores MessageBox cancel/close signals", () => {
    notifyError("cancel", "fallback");
    notifyError("close", "fallback");
    expect(messageSpy).not.toHaveBeenCalled();
  });

  it("ignores cancelled requests", () => {
    notifyError(new ApiError("canceled", 0, "canceled"), "fallback");
    expect(messageSpy).not.toHaveBeenCalled();
  });

  it("ignores errors already shown to the user", () => {
    const error = new ApiError("http", 403, "No permission");
    error.handled = true;
    notifyError(error, "fallback");
    notifyError({ status: 403, message: "No permission", handled: true }, "fallback");
    expect(messageSpy).not.toHaveBeenCalled();
  });

  it("shows one grouped toast and marks the error handled", () => {
    const error = new ApiError("http", 409, "Service code already exists: demo", {
      code: "MS010",
      serverMessage: "Service code already exists: demo",
    });

    notifyError(error, "fallback");
    notifyError(error, "fallback");

    expect(messageSpy).toHaveBeenCalledTimes(1);
    expect(messageSpy).toHaveBeenCalledWith({
      type: "error",
      message: "Service code already exists: demo",
      grouping: true,
      showClose: true,
      duration: 4500,
    });
    expect(isHandledError(error)).toBe(true);
  });

  it("uses the fallback when the error has no message", () => {
    notifyError(null, "fallback");
    expect(messageSpy).toHaveBeenCalledWith(expect.objectContaining({ message: "fallback" }));
  });
});
