package com.dati.mcp.domain.service;

import com.dati.common.JsonUtils;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.service.ColumnService;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.model.param.UpdateColumnInfoArgs;
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
 * UPDATE_COLUMN_INFO: writes column description/aliases into the shared
 * metadata store. Omitted write fields keep current values (ColumnService is
 * null-guarded); the reported "new" value reflects the merged state.
 */
@Component
public class UpdateColumnInfoExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final MetadataEntityResolver resolver;
    private final ColumnService columnService;
    private final McpMetadataAuditLogDAO auditLogDAO;

    public UpdateColumnInfoExecutor(ScopeValidator scopeValidator,
                                    MetadataEntityResolver resolver,
                                    ColumnService columnService,
                                    McpMetadataAuditLogDAO auditLogDAO) {
        this.scopeValidator = scopeValidator;
        this.resolver = resolver;
        this.columnService = columnService;
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.UPDATE_COLUMN_INFO;
    }

    @Override
    @Transactional
    public ToolTestData execute(ToolExecutionContext ctx) {
        UpdateColumnInfoArgs args = ctx.args(UpdateColumnInfoArgs.class);
        List<MetadataUpdateResult> results = new ArrayList<>();
        for (UpdateColumnInfoArgs.UpdateColumnItem item : dedupe(args)) {
            results.add(updateOne(ctx, item));
        }
        return new MetadataUpdateData(results);
    }

    /** In-call dedupe by locate key (dsId + schema + table + column). */
    private List<UpdateColumnInfoArgs.UpdateColumnItem> dedupe(UpdateColumnInfoArgs args) {
        LinkedHashMap<String, UpdateColumnInfoArgs.UpdateColumnItem> unique = new LinkedHashMap<>();
        for (UpdateColumnInfoArgs.UpdateColumnItem item : args.columns()) {
            unique.putIfAbsent(key(item.dataSourceId(), item.schema(), item.table(), item.column()), item);
        }
        return new ArrayList<>(unique.values());
    }

    private static String key(String dsId, String schema, String table, String column) {
        return dsId + '\u0000' + Objects.toString(schema, "") + '\u0000' + table + '\u0000' + column;
    }

    private MetadataUpdateResult updateOne(ToolExecutionContext ctx,
                                           UpdateColumnInfoArgs.UpdateColumnItem item) {
        try {
            scopeValidator.validateDataSource(ctx.scopeItems(), item.dataSourceId());
            MetadataEntityResolver.ColumnTarget target = resolver
                .resolveColumn(item.dataSourceId(), item.schema(), item.table(), item.column())
                .orElseThrow(() -> new ToolExecuteException(ToolError.ENTITY_NOT_FOUND,
                    "column " + item.dataSourceId() + "." + item.table() + "." + item.column()));

            Map<String, Object> old = valueMap(target.description(), target.aliases());
            ColumnInfo update = new ColumnInfo();
            update.setDescription(item.description());
            update.setAliases(item.aliases());
            columnService.updateColumn(target.columnId(), update);
            Map<String, Object> neu = valueMap(
                item.description() != null ? item.description() : target.description(),
                item.aliases() != null ? item.aliases() : target.aliases());

            saveAudit(ctx, "COLUMN", target.columnId(), item.column(), "UPDATE", old, neu);
            return new MetadataUpdateResult("COLUMN", item.column(), true, "UPDATE", old, neu, null);
        } catch (ToolExecuteException e) {
            return new MetadataUpdateResult("COLUMN", item.column(), false, null, null, null,
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
