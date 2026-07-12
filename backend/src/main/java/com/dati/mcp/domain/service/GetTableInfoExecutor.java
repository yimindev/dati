package com.dati.mcp.domain.service;

import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.server.pojo.TableDef;
import com.dati.mcp.server.pojo.TableMetadata;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetTableInfoExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final TableMetadataService tableMetadataService;

    public GetTableInfoExecutor(ScopeValidator scopeValidator,
                                 TableMetadataService tableMetadataService) {
        this.scopeValidator = scopeValidator;
        this.tableMetadataService = tableMetadataService;
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

        scopeValidator.validate(ctx.scopeItems(), dsId, Set.of(), null);

        List<TableDef> entries = new ArrayList<>();
        for (Map<String, Object> entry : rawTables) {
            String schema = (String) entry.get("schema");
            String tableName = requireNonBlank(entry, "table");
            tableMetadataService.getTableMeta(dsId, schema, tableName)
                .map(tm -> new TableDef(tableName, schema, tm.description(),
                        tm.aliases(), tm.columns()))
                .ifPresent(entries::add);
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
}
