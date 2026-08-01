package com.dati.mcp.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool 配置接口。每个 {@link McpToolType} 对应一个内层实现类。
 * <p>
 * 反序列化路由：
 * <ul>
 *   <li>已知具体类（Mapper/Assembler）→ 直接 {@code JsonUtils.fromJson(json, XxxConfig.class)}</li>
 *   <li>快照整体反序列化（接口字段）→ 由 {@code McpServiceSnapshotMapper}
 *       根据父级 {@code tool_type} 显式确定类后反序列化</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ToolConfig {

    @Data
    class SearchMetadataConfig implements ToolConfig {
        private int timeout = 30;
    }

    @Data
    class GetTableInfoConfig implements ToolConfig {
        private int timeout = 30;
    }

    @Data
    class ExecuteSqlConfig implements ToolConfig {
        private SqlPolicy sqlPolicy = new SqlPolicy();
        private int timeout = 30;
        private int maxRows = 1000;
    }

    @Data
    class ParamSqlConfig implements ToolConfig {
        private String dataSourceId;
        private String sqlTemplate;
        private List<ToolParameter> parameters = new ArrayList<>();
        private SqlPolicy sqlPolicy = new SqlPolicy();
        private int timeout = 30;
        private int maxRows = 1000;
    }
}
