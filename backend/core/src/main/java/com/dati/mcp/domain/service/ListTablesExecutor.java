package com.dati.mcp.domain.service;

import com.dati.datasource.domain.model.DataSourceDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.repository.dao.TableInfoDAO;
import com.dati.datasource.repository.po.TableInfoPO;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.server.pojo.TableListData;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LIST_TABLES executor: lists the full table inventory (schema/name/description/aliases)
 * of all data sources in the service's data scope. Table-level only — the LLM is expected
 * to follow up with GET_TABLE_INFO for column details.
 */
@Component
public class ListTablesExecutor implements ToolExecutor {

    private final McpServiceDataScopeService dataScopeService;
    private final TableInfoDAO tableInfoDAO;
    private final DataSourceService dataSourceService;

    public ListTablesExecutor(McpServiceDataScopeService dataScopeService,
                              TableInfoDAO tableInfoDAO,
                              DataSourceService dataSourceService) {
        this.dataScopeService = dataScopeService;
        this.tableInfoDAO = tableInfoDAO;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.LIST_TABLES;
    }

    @Override
    public ToolTestData execute(ToolExecutionContext ctx) {
        if (ctx.scopeItems().isEmpty()) {
            return new TableListData(List.of());
        }
        Set<String> dsIds = dataScopeService.getResolvedDataSourceIds(ctx.serviceId());
        if (dsIds.isEmpty()) {
            return new TableListData(List.of());
        }
        Map<String, DataSourceService.DsBrief> briefs = dataSourceService.getDataSourceBriefs(dsIds);
        List<DataSourceDef> groups = new ArrayList<>();
        for (String dsId : dsIds) {
            List<TableInfoPO> tables = tableInfoDAO.findByDataSourceId(dsId);
            if (tables.isEmpty()) {
                continue;
            }
            DataSourceService.DsBrief brief = briefs.get(dsId);
            List<TableDef> tableDefs = tables.stream()
                .map(t -> new TableDef(t.getName(), t.getSchema(), t.getDescription(), t.getAliases(), null))
                .toList();
            groups.add(new DataSourceDef(dsId,
                brief != null ? brief.name() : dsId,
                brief != null && brief.dbType() != null ? brief.dbType().name() : null,
                brief != null ? brief.defaultSchema() : null,
                brief != null ? brief.description() : null,
                tableDefs));
        }
        return new TableListData(groups);
    }
}
