import { get, post, del } from "./http";
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  User,
  IdResponse,
  ApiKey,
  ApiKeyCreated,
} from "./types";

export async function login(
  type: string,
  name: string,
  password: string
): Promise<LoginResponse> {
  return post<LoginResponse>("/v1/auth/login", {
    type,
    name,
    password,
  } as LoginRequest);
}

export async function register(
  request: RegisterRequest
): Promise<IdResponse> {
  return post<IdResponse>("/v1/auth/register", request);
}

export async function me(): Promise<User> {
  return get<User>("/v1/auth/me");
}

export async function createApiKey(
  name: string,
  expiresInDays?: number
): Promise<ApiKeyCreated> {
  return post<ApiKeyCreated>("/v1/auth/api-keys", {
    name,
    expires_in_days: expiresInDays,
  });
}

export async function listApiKeys(): Promise<ApiKey[]> {
  return get<ApiKey[]>("/v1/auth/api-keys");
}

export async function deleteApiKey(id: string): Promise<void> {
  return del<void>(`/v1/auth/api-keys/${id}`);
}
