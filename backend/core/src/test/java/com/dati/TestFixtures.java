package com.dati;

import com.dati.common.JsonUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.repository.po.ColumnInfoPO;
import com.dati.datasource.repository.po.DataSourcePO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.DbType;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpServiceStatus;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.PromptParameter;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.repository.po.McpServicePO;
import com.dati.mcp.repository.po.McpServiceSnapshotPO;

import java.time.Instant;
import java.util.List;

public class TestFixtures {

    public static final String TEST_USER_ID = "test-user-001";
    public static final String TEST_DATASOURCE_ID = "ds-001";
    public static final String TEST_TABLE_ID = "table-001";
    public static final String TEST_COLUMN_ID = "col-001";

    public static DataSource createTestDataSource() {
        DataSource ds = new DataSource();
        ds.setId(TEST_DATASOURCE_ID);
        ds.setName("Test MySQL DataSource");
        ds.setDescription("Test data source for unit tests");
        ds.setType(DbType.MYSQL);
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");
        ds.setUsername("root");
        ds.setPassword("password123");
        ds.setDefaultSchema("public");
        ds.setCreatedBy(TEST_USER_ID);
        ds.setCreatedAt(Instant.now());
        ds.setUpdatedBy(TEST_USER_ID);
        ds.setUpdatedAt(Instant.now());
        return ds;
    }

    public static DataSourcePO createTestDataSourcePO() {
        DataSourcePO po = new DataSourcePO();
        po.setId(TEST_DATASOURCE_ID);
        po.setName("Test MySQL DataSource");
        po.setDescription("Test data source for unit tests");
        po.setType(DbType.MYSQL);
        po.setJdbcUrl("jdbc:mysql://localhost:3306/test_db");
        po.setUserName("root");
        po.setEncryptedPassword("password123");
        po.setDefaultSchema("public");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static TableInfo createTestTableInfo() {
        TableInfo table = new TableInfo();
        table.setId(TEST_TABLE_ID);
        table.setName("test_table");
        table.setDescription("Test table");
        table.setDatasourceId(TEST_DATASOURCE_ID);
        table.setSchema("public");
        table.setCreatedBy(TEST_USER_ID);
        table.setCreatedAt(Instant.now());
        table.setUpdatedBy(TEST_USER_ID);
        table.setUpdatedAt(Instant.now());
        return table;
    }

    public static TableInfoPO createTestTableInfoPO() {
        TableInfoPO po = new TableInfoPO();
        po.setId(TEST_TABLE_ID);
        po.setName("test_table");
        po.setDescription("Test table");
        po.setDataSourceId(TEST_DATASOURCE_ID);
        po.setSchema("public");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static ColumnInfo createTestColumnInfo() {
        ColumnInfo column = new ColumnInfo();
        column.setId(TEST_COLUMN_ID);
        column.setName("test_column");
        column.setDescription("Test column description");
        column.setTableId(TEST_TABLE_ID);
        column.setColumnType("VARCHAR(255)");
        column.setCreatedBy(TEST_USER_ID);
        column.setCreatedAt(Instant.now());
        column.setUpdatedBy(TEST_USER_ID);
        column.setUpdatedAt(Instant.now());
        return column;
    }

    public static ColumnInfoPO createTestColumnInfoPO() {
        ColumnInfoPO po = new ColumnInfoPO();
        po.setId(TEST_COLUMN_ID);
        po.setName("test_column");
        po.setDescription("Test column description");
        po.setTableId(TEST_TABLE_ID);
        po.setColumnType("VARCHAR(255)");
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static final String TEST_MCP_SERVICE_ID = "mcp-svc-001";
    public static final String TEST_MCP_SERVICE_CODE = "test-mcp-service";
    public static final String TEST_MCP_CUSTOM_TOOL_ID = "mcp-ct-001";
    public static final String TEST_MCP_PROMPT_ID = "mcp-pt-001";

    public static McpService createTestMcpService() {
        McpService service = new McpService();
        service.setId(TEST_MCP_SERVICE_ID);
        service.setCode(TEST_MCP_SERVICE_CODE);
        service.setName("Test MCP Service");
        service.setDescription("Test MCP service for unit tests");
        service.setStatus(McpServiceStatus.DRAFT);
        service.setCreatedBy(TEST_USER_ID);
        service.setCreatedAt(Instant.now());
        service.setUpdatedBy(TEST_USER_ID);
        service.setUpdatedAt(Instant.now());
        return service;
    }

    public static McpServicePO createTestMcpServicePO() {
        McpServicePO po = new McpServicePO();
        po.setId(TEST_MCP_SERVICE_ID);
        po.setCode(TEST_MCP_SERVICE_CODE);
        po.setName("Test MCP Service");
        po.setDescription("Test MCP service for unit tests");
        po.setStatus(McpServiceStatus.DRAFT);
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static McpServiceDataScope createTestDataScope() {
        McpServiceDataScope scope = new McpServiceDataScope();
        scope.setServiceId(TEST_MCP_SERVICE_ID);
        scope.setScopeType(McpDataScopeType.DATA_SOURCE);
        scope.setReferenceId(TEST_DATASOURCE_ID);
        return scope;
    }

    public static McpCustomTool createTestCustomTool() {
        McpCustomTool tool = new McpCustomTool();
        tool.setId(TEST_MCP_CUSTOM_TOOL_ID);
        tool.setServiceId(TEST_MCP_SERVICE_ID);
        tool.setToolType(McpToolType.PARAMETERIZED_SQL);
        tool.setName("list_tasks");
        tool.setTitle("查询任务列表");
        tool.setDescription("按状态查询所有任务");
        tool.setEnabled(true);
        tool.setConfig(createTestParamSqlConfig());
        tool.setCreatedBy(TEST_USER_ID);
        tool.setCreatedAt(Instant.now());
        tool.setUpdatedBy(TEST_USER_ID);
        tool.setUpdatedAt(Instant.now());
        return tool;
    }

    public static ToolConfig.ParamSqlConfig createTestParamSqlConfig() {
        ToolConfig.ParamSqlConfig cfg = new ToolConfig.ParamSqlConfig();
        cfg.setDataSourceId(TEST_DATASOURCE_ID);
        cfg.setSqlTemplate("SELECT * FROM tasks WHERE status = {{status}}");
        ToolParameter status = new ToolParameter();
        status.setName("status");
        status.setType("String");
        status.setRequired(true);
        status.setDescription("task status");
        cfg.setParameters(List.of(status));
        return cfg;
    }

    public static McpPrompt createTestMcpPrompt() {
        McpPrompt prompt = new McpPrompt();
        prompt.setId(TEST_MCP_PROMPT_ID);
        prompt.setServiceId(TEST_MCP_SERVICE_ID);
        prompt.setName("analyze_table");
        prompt.setDescription("分析指定表的数据");
        prompt.setContent("请分析 {{table}} 表的数据。");
        prompt.setParameters(List.of(createTestPromptParameter("table", "表名", true)));
        prompt.setEnabled(true);
        prompt.setCreatedBy(TEST_USER_ID);
        prompt.setCreatedAt(Instant.now());
        prompt.setUpdatedBy(TEST_USER_ID);
        prompt.setUpdatedAt(Instant.now());
        return prompt;
    }

    public static PromptParameter createTestPromptParameter(String name, String desc, boolean required) {
        PromptParameter p = new PromptParameter();
        p.setName(name);
        p.setDescription(desc);
        p.setRequired(required);
        return p;
    }

    public static McpServiceSnapshot.SnapshotContent createTestSnapshotContent() {
        McpServiceSnapshot.SnapshotContent content = new McpServiceSnapshot.SnapshotContent();
        McpServiceSnapshot.ServiceInfo info = new McpServiceSnapshot.ServiceInfo();
        info.setId(TEST_MCP_SERVICE_ID);
        info.setName("Test MCP Service");
        info.setDescription("Test MCP service for unit tests");
        info.setCode(TEST_MCP_SERVICE_CODE);
        content.setServiceInfo(info);
        content.setDataScopes(List.of(
            new McpServiceSnapshot.DataScopeDraft(TEST_MCP_SERVICE_ID, McpDataScopeType.DATA_SOURCE, TEST_DATASOURCE_ID)));
        content.setPrebuiltTools(List.of(
            new McpServiceSnapshot.PrebuiltToolDraft(TEST_MCP_SERVICE_ID, McpToolType.SEARCH_METADATA, true,
                new ToolConfig.SearchMetadataConfig())));
        content.setCustomTools(List.of(
            new McpServiceSnapshot.CustomToolDraft(TEST_MCP_SERVICE_ID, "list_tasks", McpToolType.PARAMETERIZED_SQL,
                "查询任务列表", "按状态查询所有任务", true, createTestParamSqlConfig())));
        content.setPrompts(List.of(
            new McpServiceSnapshot.PromptDraft(TEST_MCP_SERVICE_ID, "analyze_table", "分析指定表的数据", true,
                "请分析 {{table}} 表的数据。", List.of(createTestPromptParameter("table", "表名", true)))));
        return content;
    }

    public static McpServiceSnapshotPO createTestSnapshotPO() {
        McpServiceSnapshotPO po = new McpServiceSnapshotPO();
        po.setId("snapshot-001");
        po.setServiceId(TEST_MCP_SERVICE_ID);
        po.setVersionNumber(1);
        po.setReleaseNote("v1");
        po.setSnapshotContent(JsonUtils.toJson(createTestSnapshotContent()));
        po.setCreatedBy(TEST_USER_ID);
        po.setCreatedAt(Instant.now());
        po.setUpdatedBy(TEST_USER_ID);
        po.setUpdatedAt(Instant.now());
        return po;
    }

    public static McpServiceSnapshot.PrebuiltToolDraft createTestPrebuiltToolDraft(
            McpToolType type, boolean enabled, ToolConfig config) {
        return new McpServiceSnapshot.PrebuiltToolDraft(TEST_MCP_SERVICE_ID, type, enabled, config);
    }

    public static McpServiceSnapshot.CustomToolDraft createTestCustomToolDraft(
            String name, McpToolType type, boolean enabled, ToolConfig config) {
        return new McpServiceSnapshot.CustomToolDraft(TEST_MCP_SERVICE_ID, name, type,
            name, "custom tool " + name, enabled, config);
    }

    public static McpServiceSnapshot.PromptDraft createTestPromptDraft(
            String name, boolean enabled, String content, List<PromptParameter> parameters) {
        return new McpServiceSnapshot.PromptDraft(TEST_MCP_SERVICE_ID, name, "prompt " + name, enabled,
            content, parameters);
    }

}
