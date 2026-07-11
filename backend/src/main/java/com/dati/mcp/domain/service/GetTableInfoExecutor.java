package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.db.Column;
import com.dati.db.HikariPoolManager;
import com.dati.db.JdbcConnector;
import com.dati.db.client.DbClient;
import com.dati.db.client.DbClientFactory;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.server.pojo.TableEntry;
import com.dati.mcp.server.pojo.TableMetadata;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.sql.Connection;
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
    @SuppressWarnings("unchecked")
    public ToolTestData execute(ToolExecutionContext ctx) {
        String dsId = requireNonBlank(ctx.arguments(), "data_source_id");
        List<Map<String, Object>> rawTables = (List<Map<String, Object>>) ctx.arguments().get("tables");
        if (rawTables == null || rawTables.isEmpty()) {
            throw new ToolExecuteException(ToolError.PARAM_MISSING, "tables");
        }

        List<TableRef> tableDefs = rawTables.stream()
            .map(m -> new TableRef(
                (String) m.get("schema"),
                requireNonBlank(m, "table")))
            .toList();

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
        try (Connection conn = HikariPoolManager.getConnection(connector)) {
            for (TableRef def : tableDefs) {
                try {
                    List<Column> columns = dbClient.getColumns(conn, null,
                        def.schema(), def.name());
                    entries.add(new TableEntry(true, def.name(), def.schema(), columns, null));
                } catch (SQLException e) {
                    entries.add(new TableEntry(false, def.name(), def.schema(), null,
                        e.getMessage()));
                }
            }
        } catch (SQLException e) {
            throw new ToolExecuteException(ToolError.SQL_EXECUTION_ERROR, e.getMessage());
        }

        return new TableMetadata(entries);
    }

    private static String requireNonBlank(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null || (val instanceof String s && s.isBlank())) {
            throw new ToolExecuteException(ToolError.PARAM_MISSING, key);
        }
        return val.toString();
    }

    private record TableRef(String schema, String name) {}
}
