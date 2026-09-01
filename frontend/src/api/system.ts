import { get } from "~/api/http.ts";

export interface DatabaseTypeVO {
  type: string;
  label: string;
  default_port: number;
  jdbc_url_template: string;
}

export type SystemConfig = {
  column_value_sample_limit: number;  // 系统默认采样数量
  column_value_length_limit: number;  // 值长度限制
  supported_database_types?: DatabaseTypeVO[]; // 支持的数据库类型
};

export async function getSystemConfig() {
  return get<SystemConfig>("/v1/system/config");
}

