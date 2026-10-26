import axios, { type AxiosInstance } from "axios";
import { ElMessage } from "element-plus";
import { i18n } from "~/plugins/i18n";
import {
  ApiError,
  isHandledError,
  normalizeError,
  notifyError,
  resolveErrorMessage,
} from "./error";

export { ApiError, isHandledError, normalizeError, notifyError };
export type { ApiErrorKind } from "./error";

// 可从 import.meta.env 加载
const BASE_URL = import.meta.env.VITE_API_BASE_URL as string | undefined;

export const http: AxiosInstance = axios.create({
  baseURL: BASE_URL ?? "",
  timeout: 15000,
  withCredentials: true,
});

function getAccessToken(): string | null {
  return localStorage.getItem("access_token");
}

/** Reports the language of the console UI to the backend for error messages. */
export function acceptLanguageHeader(locale = i18n.global.locale.value): string {
  return locale === "zh" ? "zh-CN" : "en";
}

/** Login/register/guest requests handle their own 401 errors; every other request redirects. */
export function isAuthEntryUrl(url: string | undefined): boolean {
  return Boolean(url && /\/auth\/(login|register|guest)(?:$|[?])/.test(url));
}

http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.headers) {
    config.headers["Accept-Language"] = acceptLanguageHeader();
  }
  return config;
});

let redirectingToLogin = false;

function handleUnauthorized(apiError: ApiError): void {
  localStorage.removeItem("access_token");
  if (typeof window === "undefined" || window.location.pathname === "/login") {
    return;
  }
  if (!redirectingToLogin) {
    redirectingToLogin = true;
    ElMessage({
      type: "error",
      message: resolveErrorMessage(apiError, i18n.global.t("error.http401")),
      grouping: true,
      showClose: true,
      duration: 4500,
    });
    window.setTimeout(() => {
      window.location.href = "/login";
    }, 1200);
    window.setTimeout(() => {
      redirectingToLogin = false;
    }, 5000);
  }
}

http.interceptors.response.use(
  (resp) => {
    // 后端统一返回数据结构时在这里解包；当前直接返回响应体
    return resp.data;
  },
  (error) => {
    const apiError = normalizeError(error);

    if (apiError.kind === "canceled") {
      return Promise.reject(apiError);
    }

    if (apiError.status === 401) {
      const url = error?.config?.url as string | undefined;
      if (!isAuthEntryUrl(url)) {
        apiError.handled = true;
        handleUnauthorized(apiError);
      }
      return Promise.reject(apiError);
    }

    // 403 and other errors are shown by the caller through notifyError() so the
    // precise backend code/message is preserved.
    return Promise.reject(apiError);
  },
);

// 统一导出带类型的方法
export async function get<T>(url: string, params?: Record<string, any>, signal?: AbortSignal): Promise<T> {
  return http.get(url, { params, signal });
}

export async function post<T, B = any>(url: string, body?: B, signal?: AbortSignal): Promise<T> {
  return http.post(url, body, { signal });
}

export async function put<T, B = any>(url: string, body?: B, signal?: AbortSignal): Promise<T> {
  return http.put(url, body, { signal });
}

export async function del<T>(url: string, params?: Record<string, any>, signal?: AbortSignal): Promise<T> {
  return http.delete(url, { params, signal });
}
