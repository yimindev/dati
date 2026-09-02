import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("element-plus", () => ({
  ElMessage: { error: vi.fn(), success: vi.fn(), info: vi.fn(), warning: vi.fn() },
}));

import { ElMessage } from "element-plus";
import { isHandledError, normalizeError, notifyError, type ApiError } from "../http";

const errorSpy = vi.mocked(ElMessage.error);

beforeEach(() => {
  errorSpy.mockClear();
});

describe("http API error handling", () => {
  it("isHandledError should correctly check if error has handled flag", () => {
    expect(isHandledError(null)).toBe(false);
    expect(isHandledError(undefined)).toBe(false);
    expect(isHandledError({})).toBe(false);
    expect(isHandledError({ handled: false })).toBe(false);
    expect(isHandledError({ handled: true })).toBe(true);

    const handledError: ApiError = {
      status: 403,
      message: "No permission to access this resource",
      handled: true,
    };
    expect(isHandledError(handledError)).toBe(true);
  });

  it("normalizeError should extract error details properly", () => {
    const customError = new Error("Custom failure");
    const normalized = normalizeError(customError);
    expect(normalized.status).toBe(0);
    expect(normalized.message).toBe("Custom failure");
  });

  it("notifyError should ignore ElMessageBox cancel/close signals", () => {
    notifyError("cancel", "fallback");
    notifyError("close", "fallback");
    expect(errorSpy).not.toHaveBeenCalled();
  });

  it("notifyError should skip errors already handled by the interceptor", () => {
    notifyError({ status: 403, message: "No permission", handled: true }, "fallback");
    expect(errorSpy).not.toHaveBeenCalled();
  });

  it("notifyError should prefer the error message over the fallback", () => {
    notifyError({ status: 500, message: "boom" }, "fallback");
    expect(errorSpy).toHaveBeenCalledTimes(1);
    expect(errorSpy).toHaveBeenCalledWith("boom");
  });

  it("notifyError should use the fallback when no message is present", () => {
    notifyError({ status: 500 } as ApiError, "fallback");
    notifyError(new Error("custom failure"), "other fallback");
    expect(errorSpy).toHaveBeenCalledTimes(2);
    expect(errorSpy).toHaveBeenCalledWith("fallback");
    expect(errorSpy).toHaveBeenCalledWith("custom failure");
  });
});
