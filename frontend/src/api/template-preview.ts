import { post } from "./http";

export interface TemplatePreviewRequest {
  mode: "TEXT" | "SQL";
  template: string;
  values: Record<string, any>;
}

export interface TemplatePreviewResponse {
  rendered: string;
}

export function previewTemplate(
  body: TemplatePreviewRequest,
  signal?: AbortSignal,
): Promise<TemplatePreviewResponse> {
  return post<TemplatePreviewResponse, TemplatePreviewRequest>(
    "/v1/template/preview",
    body,
    signal,
  );
}
