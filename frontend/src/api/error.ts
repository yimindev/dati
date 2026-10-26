import axios from "axios";
import { ElMessage } from "element-plus";
import { i18n } from "~/plugins/i18n";

export type ApiErrorKind = "http" | "network" | "timeout" | "canceled" | "unknown";

/**
 * Normalized API failure. `code` is the backend business code (for example `MS010`);
 * `handled` is true once a toast has already been shown for this failure.
 */
export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  readonly code?: string;
  readonly serverMessage?: string;
  readonly raw?: unknown;
  handled = false;

  constructor(
    kind: ApiErrorKind,
    status: number,
    message: string,
    options: { code?: string; serverMessage?: string; raw?: unknown } = {},
  ) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = status;
    this.code = options.code;
    this.serverMessage = options.serverMessage;
    this.raw = options.raw;
  }
}

function isRecord(value: unknown): value is Record<string, any> {
  return typeof value === "object" && value !== null;
}

export function isCanceledError(err: unknown): boolean {
  if (err instanceof ApiError) {
    return err.kind === "canceled";
  }
  return axios.isCancel(err) || (isRecord(err) && err.code === "ERR_CANCELED");
}

export function normalizeError(err: unknown): ApiError {
  if (err instanceof ApiError) {
    return err;
  }

  if (axios.isAxiosError(err)) {
    if (isCanceledError(err)) {
      return new ApiError("canceled", 0, "canceled", { raw: err });
    }
    if (err.code === "ECONNABORTED" || /timeout/i.test(err.message)) {
      return new ApiError("timeout", 0, err.message, { raw: err });
    }
    if (!err.response) {
      return new ApiError("network", 0, err.message || "Network Error", { raw: err });
    }

    const data = err.response.data;
    const code = isRecord(data) && typeof data.code === "string" ? data.code : undefined;
    const serverMessage =
      isRecord(data) && typeof data.message === "string" && data.message.trim()
        ? data.message.trim()
        : undefined;

    return new ApiError("http", err.response.status, serverMessage || err.message, {
      code,
      serverMessage,
      raw: err,
    });
  }

  if (err instanceof Error) {
    return new ApiError("unknown", 0, err.message, { raw: err });
  }

  return new ApiError("unknown", 0, typeof err === "string" ? err : "", { raw: err });
}

function statusMessageKey(status: number): string | undefined {
  if (status >= 500) {
    return "error.http5xx";
  }
  return {
    400: "error.http400",
    401: "error.http401",
    403: "error.http403",
    404: "error.http404",
    409: "error.http409",
    429: "error.http429",
  }[status];
}

/**
 * Resolves the text shown to the user. Backend messages are preferred; client-side
 * network/timeout/status fallbacks are localized. Error codes are only appended for
 * server errors (5xx), where they help support diagnose the failure.
 */
export function resolveErrorMessage(err: ApiError, fallback: string): string {
  if (err.kind === "canceled") {
    return "";
  }
  if (err.kind === "network") {
    return i18n.global.t("error.network");
  }
  if (err.kind === "timeout") {
    return i18n.global.t("error.timeout");
  }
  if (err.kind === "http") {
    const key = statusMessageKey(err.status);
    let message = err.serverMessage || (key ? i18n.global.t(key) : "") || fallback;
    if (!message) {
      message = i18n.global.t("error.unknown");
    }
    if (err.status >= 500 && err.code) {
      message += ` (${err.code})`;
    }
    return message;
  }
  return err.message || fallback || i18n.global.t("error.unknown");
}

export function isHandledError(err: unknown): boolean {
  return isRecord(err) && err.handled === true;
}

/**
 * Shows a single error toast for an API failure. Cancelled requests, intentionally
 * ignored MessageBox signals, and errors already shown elsewhere are skipped.
 */
export function notifyError(err: unknown, fallback: string): void {
  if (err === "cancel" || err === "close") {
    return;
  }
  if (isHandledError(err)) {
    return;
  }

  const apiError = normalizeError(err);
  if (apiError.kind === "canceled" || apiError.handled) {
    return;
  }

  const message = resolveErrorMessage(apiError, fallback);
  if (!message) {
    return;
  }

  ElMessage({
    type: "error",
    message,
    grouping: true,
    showClose: true,
    duration: 4500,
  });
  apiError.handled = true;
}
