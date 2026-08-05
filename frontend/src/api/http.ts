import axios, { AxiosError, type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { i18n } from '~/plugins/i18n'

// 可从 import.meta.env 加载
const BASE_URL = import.meta.env.VITE_API_BASE_URL as string | undefined

export interface ApiError {
  status: number
  code?: string
  message: string
  details?: unknown
  raw?: unknown
}

function normalizeError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const e = err as AxiosError<any>
    return {
      status: e.response?.status ?? 0,
      code: e.response?.data?.code,
      message: e.response?.data?.message || e.message || 'Network Error',
      details: e.response?.data?.details,
      raw: e.toJSON?.() ?? e,
    }
  }
  return { status: 0, message: (err as Error)?.message ?? 'Unknown error', raw: err }
}

export const http: AxiosInstance = axios.create({
  baseURL: BASE_URL ?? '/api',
  timeout: 15000,
  withCredentials: true,
})

// 读取/写入 token 的辅助，你可以替换为你项目的实际存储方式
function getAccessToken(): string | null {
  return localStorage.getItem('access_token')
}

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 支持 AbortController 取消
  // 调用端传入 { signal } 即可
  return config
})

http.interceptors.response.use(
  (resp) => {
    // 统一“数据解包”：后端若统一返回 { code, data, message }
    // 可在这里直接返回 resp.data.data；示例保守返回 resp.data
    return resp.data;
  },
  async (error) => {
    const err = normalizeError(error);
    if (err.status === 401) {
      const url = error.config?.url || '';
      const isAuthApi = url.includes('/auth/');
      const currentPath = window.location.pathname;
      
      if (!isAuthApi && currentPath !== '/login') {
        localStorage.removeItem("access_token");
        window.location.href = "/login";
      }
      return Promise.reject(err);
    }
    if (err.status === 403) {
      // 权限不足：提示但不登出、不清 token（后端是唯一权威，列表已静默过滤）
      ElMessage.error(i18n.global.t("common.noPermission"));
      return Promise.reject(err);
    }
    return Promise.reject(err);
  },
);

// 统一导出带类型的方法
export async function get<T>(url: string, params?: Record<string, any>, signal?: AbortSignal): Promise<T> {
  return http.get(url, { params, signal })
}

export async function post<T, B = any>(url: string, body?: B, signal?: AbortSignal): Promise<T> {
  return http.post(url, body, { signal })
}

export async function put<T, B = any>(url: string, body?: B, signal?: AbortSignal): Promise<T> {
  return http.put(url, body, { signal })
}

export async function del<T>(url: string, params?: Record<string, any>, signal?: AbortSignal): Promise<T> {
  return http.delete(url, { params, signal })
}

export { normalizeError }
