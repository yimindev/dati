import type { BaseResourceVO, IdResponse, PageResponse } from "~/api/types.ts";
import type { TableInfoVO } from "~/api/tableinfo";
import { get, post, put, del } from "./http";

export type { TableInfoVO };

export interface SubjectVO extends BaseResourceVO {
  datasource_id: string;
  datasource_name?: string;
  table_count?: number;
  aliases?: string[];
}

export interface SubjectAvailableTableVO {
  table_id: string;
  table_name: string;
  schema?: string;
  description?: string;
}

export interface TermVO extends BaseResourceVO {
  subject_id: string;
  aliases?: string[];
}

export interface TermRelationVO {
  id: string;
  term_id: string;
  entity_type: 'TABLE' | 'FIELD';
  table_id: string;
  table_name?: string;
  schema?: string;
  field_name?: string;
}

export interface CreateSubjectRequest {
  name: string;
  description?: string;
  datasource_id: string;
  aliases?: string[];
}

export interface UpdateSubjectRequest {
  name?: string;
  description?: string;
  aliases?: string[];
}

export interface AddTableToSubjectRequest {
  table_id: string;
}

export interface CreateTermRequest {
  name: string;
  description?: string;
  aliases?: string[];
}

export interface UpdateTermRequest {
  name?: string;
  description?: string;
  aliases?: string[];
}

export interface LinkTermRelationRequest {
  entity_type: 'TABLE' | 'FIELD';
  table_id: string;
  field_name?: string;
}

export function listSubjects(
  page: number,
  size: number,
  keyword?: string,
  signal?: AbortSignal,
): Promise<PageResponse<SubjectVO>> {
  return get<PageResponse<SubjectVO>>(
    "/v1/subjects",
    { page, size, keyword },
    signal,
  );
}

export function getSubject(id: string, signal?: AbortSignal): Promise<SubjectVO> {
  return get<SubjectVO>(`/v1/subjects/${encodeURIComponent(id)}`, undefined, signal);
}

export function createSubject(body: CreateSubjectRequest, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, CreateSubjectRequest>("/v1/subjects", body, signal);
}

export function updateSubject(id: string, body: UpdateSubjectRequest, signal?: AbortSignal): Promise<IdResponse> {
  return put<IdResponse, UpdateSubjectRequest>(`/v1/subjects/${encodeURIComponent(id)}`, body, signal);
}

export function deleteSubject(id: string, signal?: AbortSignal): Promise<IdResponse> {
  return del<IdResponse>(`/v1/subjects/${encodeURIComponent(id)}`, undefined, signal);
}

export function getSubjectTables(subjectId: string, signal?: AbortSignal): Promise<TableInfoVO[]> {
  return get<TableInfoVO[]>(`/v1/subjects/${encodeURIComponent(subjectId)}/tables`, undefined, signal);
}

export function getAvailableTables(subjectId: string, schema: string, signal?: AbortSignal): Promise<SubjectAvailableTableVO[]> {
  return get<SubjectAvailableTableVO[]>(
    `/v1/subjects/${encodeURIComponent(subjectId)}/available-tables`,
    { schema },
    signal
  );
}

export function addTableToSubject(subjectId: string, body: AddTableToSubjectRequest, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, AddTableToSubjectRequest>(`/v1/subjects/${encodeURIComponent(subjectId)}/tables`, body, signal);
}

export function removeTableFromSubject(subjectId: string, tableId: string, signal?: AbortSignal): Promise<IdResponse> {
  return del<IdResponse>(`/v1/subjects/${encodeURIComponent(subjectId)}/tables/${encodeURIComponent(tableId)}`, undefined, signal);
}

export function getTermsBySubject(subjectId: string, signal?: AbortSignal): Promise<TermVO[]> {
  return get<TermVO[]>(`/v1/subjects/${encodeURIComponent(subjectId)}/terms`, undefined, signal);
}

export function createTerm(subjectId: string, body: CreateTermRequest, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, CreateTermRequest>(`/v1/subjects/${encodeURIComponent(subjectId)}/terms`, body, signal);
}

export function updateTerm(id: string, body: UpdateTermRequest, signal?: AbortSignal): Promise<IdResponse> {
  return put<IdResponse, UpdateTermRequest>(`/v1/terms/${encodeURIComponent(id)}`, body, signal);
}

export function deleteTerm(id: string, signal?: AbortSignal): Promise<IdResponse> {
  return del<IdResponse>(`/v1/terms/${encodeURIComponent(id)}`, undefined, signal);
}

export function getTermDetail(id: string, signal?: AbortSignal): Promise<TermVO & { relations: TermRelationVO[] }> {
  return get<TermVO & { relations: TermRelationVO[] }>(`/v1/terms/${encodeURIComponent(id)}`, undefined, signal);
}

export function linkTermRelation(termId: string, body: LinkTermRelationRequest, signal?: AbortSignal): Promise<IdResponse> {
  return post<IdResponse, LinkTermRelationRequest>(`/v1/terms/${encodeURIComponent(termId)}/relations`, body, signal);
}

export function unlinkTermRelation(
  termId: string,
  tableId: string,
  fieldName: string | null,
  signal?: AbortSignal
): Promise<IdResponse> {
  const encodedTableId = encodeURIComponent(tableId);
  const encodedFieldName = encodeURIComponent(fieldName === null ? '_' : fieldName);
  return del<IdResponse>(
    `/v1/terms/${encodeURIComponent(termId)}/relations/${encodedTableId}/${encodedFieldName}`,
    undefined,
    signal
  );
}
