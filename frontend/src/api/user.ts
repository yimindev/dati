import { get } from "./http";
import type { UserBrief } from "./types";

export function searchUsers(keyword: string): Promise<UserBrief[]> {
  return get<UserBrief[]>("/v1/users/search", { keyword });
}
