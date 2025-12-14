import type { BaseResourceVO, PageResponse } from "~/api/types.ts";
import { get } from "./http";

export interface TableInfoVO extends BaseResourceVO {
  schema: string;
  display_name: string;
  datasource_id: string;
}

export function listTableInfos(datasourceId: string, page: number, size: number, keyword?: string){
  return get<PageResponse<TableInfoVO>>(
    "/v1/data-sources/" + datasourceId + "/tables",
    { page, size, keyword },
  );
}
