package com.dati.mcp.domain.service;

import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
import com.dati.datasource.domain.model.TableDef;
import com.dati.mcp.server.pojo.TableMetadata;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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
    public ToolTestData execute(ToolExecutionContext ctx) {
        GetTableInfoArgs args = ctx.args(GetTableInfoArgs.class);
        List<TableDef> entries = new ArrayList<>();
        for (GetTableInfoArgs.TableRef ref : args.tables()) {
            scopeValidator.validate(ctx.scopeItems(), ref.dataSourceId(), Set.of(), null);
            tableMetadataService.getTableMeta(ref.dataSourceId(), ref.schema(), ref.table())
                .map(tm -> new TableDef(ref.table(), ref.schema(), tm.description(),
                        tm.aliases(), tm.columns()))
                .ifPresent(entries::add);
        }
        return new TableMetadata(entries);
    }
}
