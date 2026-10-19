package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.db.Table;
import com.dati.mcp.domain.model.McpDataScopeType;
import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.server.pojo.TableListData;
import com.dati.mcp.server.pojo.ToolTestData;
import com.dati.semantic.domain.model.TermDef;
import com.dati.semantic.domain.service.TermService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LIST_TABLES executor: lists the full table inventory (schema/name/description/aliases)
 * of all data sources in the service's data scope, plus all business terms under subjects
 * in the data scope. Table-level only — the LLM is expected to follow up with
 * GET_TABLE_INFO (get_table_schema) for column details.
 */
@Slf4j
@Component
public class ListTablesExecutor implements ToolExecutor {

    private final McpServiceDataScopeService dataScopeService;
    private final TableInfoDAO tableInfoDAO;
    private final DataSourceService dataSourceService;
    private final TermService termService;
    private final JdbcMetaService jdbcMetaService;

    public ListTablesExecutor(McpServiceDataScopeService dataScopeService,
                              TableInfoDAO tableInfoDAO,
                              DataSourceService dataSourceService,
                              TermService termService,
                              JdbcMetaService jdbcMetaService) {
        this.dataScopeService = dataScopeService;
        this.tableInfoDAO = tableInfoDAO;
        this.dataSourceService = dataSourceService;
        this.termService = termService;
        this.jdbcMetaService = jdbcMetaService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.LIST_TABLES;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        if (ctx.scopeItems().isEmpty()) {
            return new TableListData(List.of(), List.of());
        }

        List<String> subjectIds = ctx.scopeItems().stream()
                .filter(s -> s.getScopeType() == McpDataScopeType.SUBJECT)
                .map(McpServiceDataScope::getReferenceId)
                .distinct()
                .toList();

        List<TermDef> termDefs = List.of();
        if (!subjectIds.isEmpty()) {
            termDefs = termService.getTermsBySubjectIds(subjectIds).stream()
                    .map(t -> new TermDef(t.name(), t.description(), t.subjectName()))
                    .toList();
        }

        Set<String> dsIds = dataScopeService.getResolvedDataSourceIds(ctx.serviceId());
        if (dsIds.isEmpty()) {
            return new TableListData(List.of(), termDefs);
        }

        Map<String, DataSourceService.DsBrief> briefs = dataSourceService.getDataSourceBriefs(dsIds);
        List<DataSourceDef> groups = new ArrayList<>();
        for (String dsId : dsIds) {
            List<TableInfoPO> importedTables = tableInfoDAO.findByDataSourceId(dsId);
            Set<String> allowedSchemas = McpDataScopeSchemas.allowedSchemas(ctx.scopeItems(), dsId);
            List<TableDef> tableDefs = buildTableDefs(dsId, importedTables, allowedSchemas);
            if (tableDefs.isEmpty()) {
                continue;
            }
            DataSourceService.DsBrief brief = briefs.get(dsId);
            groups.add(new DataSourceDef(dsId,
                brief != null ? brief.name() : dsId,
                brief != null && brief.dbType() != null ? brief.dbType().name() : null,
                brief != null ? brief.defaultSchema() : null,
                brief != null ? brief.description() : null,
                tableDefs));
        }
        return new TableListData(groups, termDefs);
    }

    private List<TableDef> buildTableDefs(String dsId, List<TableInfoPO> importedTables,
                                          Set<String> allowedSchemas) {
        List<TableDef> tableDefs = new ArrayList<>();
        Set<TableKey> seen = new LinkedHashSet<>();

        for (TableInfoPO table : importedTables) {
            TableKey key = new TableKey(table.getSchema(), table.getName());
            if (seen.add(key)) {
                tableDefs.add(new TableDef(table.getName(), table.getSchema(),
                    table.getDescription(), table.getAliases(), null));
            }
        }

        for (String schema : allowedSchemas) {
            try {
                for (Table table : jdbcMetaService.getTables(dsId, schema)) {
                    TableKey key = new TableKey(schema, table.name());
                    if (seen.add(key)) {
                        tableDefs.add(new TableDef(table.name(), schema, table.comment(),
                            List.of(), null));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to read physical tables for data source {} schema {}: {}",
                    dsId, schema, e.getMessage());
            }
        }

        return tableDefs;
    }

    private record TableKey(String schema, String name) {
    }
}
