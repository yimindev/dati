package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.Column;
import com.dati.db.JdbcConnector;
import com.dati.db.client.DbClient;
import com.dati.db.client.DbClientFactory;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.server.pojo.ColumnDef;
import com.dati.mcp.server.pojo.TableEntry;
import com.dati.mcp.server.pojo.TableMetadata;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetTableInfoExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final DataSourceService dataSourceService;

    public GetTableInfoExecutor(ScopeValidator scopeValidator, DataSourceService dataSourceService) {
        this.scopeValidator = scopeValidator;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.GET_TABLE_INFO;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        String dsId = (String) ctx.arguments().get("data_source_id");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tables = (List<Map<String, Object>>) ctx.arguments().get("tables");

        if (dsId == null || dsId.isBlank()) {
            throw new ToolExecuteException(ToolError.PARAM_MISSING, "data_source_id");
        }
        if (tables == null || tables.isEmpty()) {
            throw new ToolExecuteException(ToolError.PARAM_MISSING, "tables");
        }

        scopeValidator.validate(ctx.scopeItems(), dsId, Set.of(), null);

        DataSource dataSource = dataSourceService.getDataSource(dsId)
            .orElseThrow(() -> new ToolExecuteException(ToolError.DATA_SOURCE_NOT_FOUND, dsId));
        JdbcConnector connector = new JdbcConnector(dataSource);

        DbClient dbClient = DbClientFactory.getDbClient(dataSource.getType());
        if (dbClient == null) {
            throw new IllegalStateException(
                "No DbClient for database type: " + dataSource.getType());
        }

        List<TableEntry> entries = new ArrayList<>();
        for (Map<String, Object> entry : tables) {
            String schema = (String) entry.get("schema");
            String tableName = (String) entry.get("table");
            try {
                List<Column> columns = dbClient.getColumns(connector, null, schema, tableName);
                List<ColumnDef> columnDefs = columns.stream()
                        .map(c -> new ColumnDef(c.name(), c.type(), c.comment()))
                        .toList();
                entries.add(new TableEntry(true, tableName, schema, columnDefs, null));
            } catch (Exception e) {
                entries.add(new TableEntry(false, tableName, schema, null,
                        e instanceof SQLException ? e.getMessage() : "Error fetching table info: " + e.getMessage()));
            }
        }

        return new TableMetadata(entries);
    }
}
