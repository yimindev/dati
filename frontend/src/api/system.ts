import { get } from "~/api/http.ts";

export type SystemConfig = {
  column_value_sample_limit: number;  // 系统默认采样数量
  column_value_length_limit: number;  // 值长度限制
};

export async function getSystemConfig() {
  return get<SystemConfig>("/v1/system/config");
}
