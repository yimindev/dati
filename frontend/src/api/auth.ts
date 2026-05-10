import { post, get } from "./http";
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  User,
  IdResponse,
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
