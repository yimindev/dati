package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.TableMetadataService;
import com.dati.datasource.domain.service.TableMetadataService.TableMeta;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.param.GetTableInfoArgs;
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
    private final DataSourceService dataSourceService;

    public GetTableInfoExecutor(ScopeValidator scopeValidator,
                                 TableMetadataService tableMetadataService,
                                 DataSourceService dataSourceService) {
        this.scopeValidator = scopeValidator;
        this.tableMetadataService = tableMetadataService;
        this.dataSourceService = dataSourceService;
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
            scopeValidator.validateDataSource(ctx.scopeItems(), ref.dataSourceId());

            TableMeta meta = tableMetadataService
                    .getTableMeta(ref.dataSourceId(), ref.schema(), ref.table())
                    .orElse(null);
            boolean physical = false;

            if (meta == null) {
                Set<String> allowedSchemas = McpDataScopeSchemas.allowedSchemas(
                        ctx.scopeItems(), ref.dataSourceId());
                if (!allowedSchemas.isEmpty()) {
                    String effectiveSchema = McpDataScopeSchemas.effectiveSchema(ref.schema(), null);
                    if (effectiveSchema == null) {
                        effectiveSchema = defaultSchema(ref.dataSourceId());
                    }
                    if (effectiveSchema != null && allowedSchemas.contains(effectiveSchema)) {
                        meta = tableMetadataService
                                .getPhysicalTableMeta(ref.dataSourceId(), effectiveSchema, ref.table())
                                .orElse(null);
                        physical = meta != null;
                    }
                }
            }

            if (meta != null) {
                entries.add(toTableDef(ref, meta, physical));
            }
        }
        return new TableMetadata(entries);
    }

    private TableDef toTableDef(GetTableInfoArgs.TableRef ref, TableMeta meta, boolean physical) {
        return new TableDef(
                physical ? meta.tableName() : ref.table(),
                physical ? meta.schema() : ref.schema(),
                meta.description(), meta.aliases(), meta.columns());
    }

    private String defaultSchema(String dsId) {
        return dataSourceService.getDataSourceInternal(dsId)
                .map(DataSource::getDefaultSchema)
                .orElse(null);
    }
}
