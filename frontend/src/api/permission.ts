import { del, get, post } from "./http";
import type { AclEntry, GrantRequest, IdResponse } from "./types";

export function listAcl(resourceType: string, resourceId: string): Promise<AclEntry[]> {
  return get<AclEntry[]>(`/v1/acls/${resourceType}/${encodeURIComponent(resourceId)}`);
}

export function grantAcl(resourceType: string, resourceId: string, data: GrantRequest): Promise<IdResponse> {
  return post<IdResponse, GrantRequest>(`/v1/acls/${resourceType}/${encodeURIComponent(resourceId)}`, data);
}

export function revokeAcl(resourceType: string, resourceId: string, principalId: string, principalType = 'USER'): Promise<IdResponse> {
  return del<IdResponse>(`/v1/acls/${resourceType}/${encodeURIComponent(resourceId)}/${principalType}/${encodeURIComponent(principalId)}`);
}
