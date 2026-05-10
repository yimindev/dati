export interface IdResponse {
  id: string;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
  total_pages: number;
}


export interface BaseResourceVO {
  id: string;
  name: string;
  description?: string | null;

  created_by?: string | null;
  created_at?: string | null; // ISO 8601 datetime string
  updated_by?: string | null;
  updated_at?: string | null;

  created_user_name?: string | null;
  updated_user_name?: string | null;
}

export interface LoginRequest {
  type: string;
  name: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  password: string;
  display_name?: string;
}

export interface LoginResponse {
  token: string;
}

export interface User {
  id: string;
  name: string;
  display_name?: string;
}

