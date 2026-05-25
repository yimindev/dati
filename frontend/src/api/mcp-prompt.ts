import type { IdResponse } from "./types.ts";
import { get, post, put, del } from "./http";

export interface PromptParameter {
  name: string;
  description?: string;
  required: boolean;
}

export interface McpPromptVO {
  id: string;
  service_id: string;
  name: string;
  description?: string;
  enabled: boolean;
  content: string;
  parameters: PromptParameter[];
}

export interface McpPromptPayload {
  name: string;
  description?: string;
  enabled?: boolean;
  content: string;
  parameters?: PromptParameter[];
}

export function listPrompts(serviceId: string, signal?: AbortSignal): Promise<McpPromptVO[]> {
  return get(`/v1/mcp-services/${encodeURIComponent(serviceId)}/prompts`, undefined, signal);
}

export function createPrompt(serviceId: string, body: McpPromptPayload, signal?: AbortSignal): Promise<IdResponse> {
  return post(`/v1/mcp-services/${encodeURIComponent(serviceId)}/prompts`, body, signal);
}

export function updatePrompt(serviceId: string, promptId: string, body: McpPromptPayload, signal?: AbortSignal): Promise<IdResponse> {
  return put(`/v1/mcp-services/${encodeURIComponent(serviceId)}/prompts/${encodeURIComponent(promptId)}`, body, signal);
}

export function deletePrompt(serviceId: string, promptId: string, signal?: AbortSignal): Promise<IdResponse> {
  return del(`/v1/mcp-services/${encodeURIComponent(serviceId)}/prompts/${encodeURIComponent(promptId)}`, undefined, signal);
}
