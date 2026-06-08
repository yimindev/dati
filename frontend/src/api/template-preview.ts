import { post } from "./http";

export interface TemplatePreviewRequest {
  mode: "TEXT" | "SQL";
  template: string;
  values: Record<string, any>;
}

export interface TemplatePreviewResponse {
  rendered: string;
}

export interface TemplateExtractRequest {
  template: string;
}

export interface TemplateExtractResponse {
  variables: string[];
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

export function extractTemplateVariables(
  body: TemplateExtractRequest,
): Promise<TemplateExtractResponse> {
  return post<TemplateExtractResponse, TemplateExtractRequest>(
    "/v1/template/extract",
    body,
  );
}
