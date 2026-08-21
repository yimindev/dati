package com.dati.mcp.domain.service;

import com.dati.common.JsonUtils;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.domain.service.TableService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.UpdateTableInfoArgs;
import com.dati.mcp.repository.dao.McpMetadataAuditLogDAO;
import com.dati.mcp.repository.po.McpMetadataAuditLogPO;
import com.dati.mcp.server.pojo.MetadataUpdateData;
import com.dati.mcp.server.pojo.MetadataUpdateResult;
import com.dati.mcp.server.pojo.ToolTestData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UPDATE_TABLE_INFO: writes table description/aliases into the shared metadata
 * store. Omitted write fields keep current values. Each item is independent —
 * a failing item is reported in results without blocking the others.
 */
@Component
public class UpdateTableInfoExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final MetadataEntityResolver resolver;
    private final TableService tableService;
    private final McpMetadataAuditLogDAO auditLogDAO;

    public UpdateTableInfoExecutor(ScopeValidator scopeValidator,
                                   MetadataEntityResolver resolver,
                                   TableService tableService,
                                   McpMetadataAuditLogDAO auditLogDAO) {
        this.scopeValidator = scopeValidator;
        this.resolver = resolver;
        this.tableService = tableService;
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.UPDATE_TABLE_INFO;
    }

    @Override
    @Transactional
    public ToolTestData execute(ToolExecutionContext ctx) {
        UpdateTableInfoArgs args = ctx.args(UpdateTableInfoArgs.class);
        List<MetadataUpdateResult> results = new ArrayList<>();
        for (UpdateTableInfoArgs.UpdateTableItem item : dedupe(args)) {
            results.add(updateOne(ctx, item));
        }
        return new MetadataUpdateData(results);
    }

    /** In-call dedupe by locate key (dsId + schema + table). */
    private List<UpdateTableInfoArgs.UpdateTableItem> dedupe(UpdateTableInfoArgs args) {
        LinkedHashMap<String, UpdateTableInfoArgs.UpdateTableItem> unique = new LinkedHashMap<>();
        for (UpdateTableInfoArgs.UpdateTableItem item : args.tables()) {
            unique.putIfAbsent(key(item.dataSourceId(), item.schema(), item.table()), item);
        }
        return new ArrayList<>(unique.values());
    }

    private static String key(String dsId, String schema, String table) {
        return dsId + '\u0000' + Objects.toString(schema, "") + '\u0000' + table;
    }

    private MetadataUpdateResult updateOne(ToolExecutionContext ctx,
                                           UpdateTableInfoArgs.UpdateTableItem item) {
        String entity = QualifiedName.of(item.schema(), item.table());
        try {
            scopeValidator.validateDataSource(ctx.scopeItems(), item.dataSourceId());
            MetadataEntityResolver.TableTarget target = resolver
                .resolveTable(item.dataSourceId(), item.schema(), item.table())
                .orElseThrow(() -> new ToolExecuteException(ToolError.ENTITY_NOT_FOUND,
                    "table " + item.dataSourceId() + "." + item.table()));

            Map<String, Object> old = valueMap(target.description(), target.aliases());
            TableInfo update = new TableInfo();
            update.setDescription(item.description() != null ? item.description() : target.description());
            update.setAliases(item.aliases() != null ? item.aliases() : target.aliases());
            tableService.updateTable(target.tableId(), update);
            Map<String, Object> neu = valueMap(update.getDescription(), update.getAliases());

            saveAudit(ctx, "TABLE", target.tableId(), item.table(), "UPDATE", old, neu);
            return new MetadataUpdateResult("TABLE", entity, true, "UPDATE", old, neu, null);
        } catch (ToolExecuteException e) {
            return new MetadataUpdateResult("TABLE", entity, false, null, null, null,
                new MetadataUpdateResult.MetadataUpdateError(e.getErrorCategory(), e.getMessage()));
        }
    }

    private void saveAudit(ToolExecutionContext ctx, String entityType, String entityId,
                           String entityName, String changeType,
                           Map<String, Object> old, Map<String, Object> neu) {
        McpMetadataAuditLogPO po = new McpMetadataAuditLogPO();
        po.setServiceId(ctx.serviceId());
        po.setToolType(getToolType().name());
        po.setEntityType(entityType);
        po.setEntityId(entityId);
        po.setEntityName(entityName);
        po.setChangeType(changeType);
        po.setOldValue(old == null ? null : JsonUtils.toJson(old));
        po.setNewValue(JsonUtils.toJson(neu));
        auditLogDAO.save(po);
    }

    private static Map<String, Object> valueMap(String description, List<String> aliases) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", description);
        map.put("aliases", aliases == null ? List.of() : aliases);
        return map;
    }
}
