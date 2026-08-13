package com.dati.mcp.domain.service;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.param.UpsertTermArgs;
import com.dati.mcp.repository.dao.McpMetadataAuditLogDAO;
import com.dati.mcp.repository.po.McpMetadataAuditLogPO;
import com.dati.mcp.server.pojo.MetadataUpdateData;
import com.dati.mcp.server.pojo.MetadataUpdateResult;
import com.dati.mcp.server.pojo.ToolTestData;
import com.dati.semantic.domain.model.Term;
import com.dati.semantic.domain.service.TermService;
import com.dati.semantic.repository.dao.TermDAO;
import com.dati.semantic.repository.po.TermPO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UPSERT_TERM: creates a business term when missing, otherwise updates its
 * description/aliases. Locate key is subjectId + name; renaming is not
 * supported (goes through the admin TermManager).
 */
@Component
public class UpsertTermExecutor implements ToolExecutor {

    private final ScopeValidator scopeValidator;
    private final TermDAO termDAO;
    private final TermService termService;
    private final McpMetadataAuditLogDAO auditLogDAO;

    public UpsertTermExecutor(ScopeValidator scopeValidator,
                              TermDAO termDAO,
                              TermService termService,
                              McpMetadataAuditLogDAO auditLogDAO) {
        this.scopeValidator = scopeValidator;
        this.termDAO = termDAO;
        this.termService = termService;
        this.auditLogDAO = auditLogDAO;
    }

    @Override
    public McpToolType getToolType() {
        return McpToolType.UPSERT_TERM;
    }

    @Override
    @Transactional
    public ToolTestData execute(ToolExecutionContext ctx) {
        UpsertTermArgs args = ctx.args(UpsertTermArgs.class);
        List<MetadataUpdateResult> results = new ArrayList<>();
        for (UpsertTermArgs.UpsertTermItem item : dedupe(args)) {
            results.add(upsertOne(ctx, item));
        }
        return new MetadataUpdateData(results);
    }

    /** In-call dedupe by locate key (subjectName + name). */
    private List<UpsertTermArgs.UpsertTermItem> dedupe(UpsertTermArgs args) {
        LinkedHashMap<String, UpsertTermArgs.UpsertTermItem> unique = new LinkedHashMap<>();
        for (UpsertTermArgs.UpsertTermItem item : args.terms()) {
            unique.putIfAbsent(item.subjectName() + '\u0000' + item.name(), item);
        }
        return new ArrayList<>(unique.values());
    }

    private MetadataUpdateResult upsertOne(ToolExecutionContext ctx,
                                           UpsertTermArgs.UpsertTermItem item) {
        try {
            String subjectId = scopeValidator.resolveSubjectInScope(ctx.scopeItems(), item.subjectName());
            Optional<TermPO> existing = termDAO.findBySubjectIdAndName(subjectId, item.name());
            return existing.map(termPO -> updateExisting(ctx, termPO, item)).orElseGet(() -> createNew(ctx, subjectId, item));
        } catch (ToolExecuteException e) {
            return new MetadataUpdateResult("TERM", item.name(), false, null, null, null,
                new MetadataUpdateResult.MetadataUpdateError(e.getErrorCategory(), e.getMessage()));
        }
    }

    private MetadataUpdateResult updateExisting(ToolExecutionContext ctx, TermPO po,
                                                UpsertTermArgs.UpsertTermItem item) {
        Map<String, Object> old = valueMap(po.getDescription(), po.getAliases());
        Term update = new Term();
        update.setDescription(item.description() != null ? item.description() : po.getDescription());
        update.setAliases(item.aliases() != null ? item.aliases() : po.getAliases());
        termService.updateTerm(po.getId(), update);
        Map<String, Object> neu = valueMap(update.getDescription(), update.getAliases());

        saveAudit(ctx, po.getId(), item.name(), "UPDATE", old, neu);
        return new MetadataUpdateResult("TERM", item.name(), true, "UPDATE", old, neu, null);
    }

    private MetadataUpdateResult createNew(ToolExecutionContext ctx, String subjectId,
                                           UpsertTermArgs.UpsertTermItem item) {
        Term create = new Term();
        create.setSubjectId(subjectId);
        create.setName(item.name());
        create.setDescription(item.description());
        create.setAliases(item.aliases() == null ? List.of() : item.aliases());
        Term created = termService.createTerm(create);
        Map<String, Object> neu = valueMap(created.getDescription(), created.getAliases());

        saveAudit(ctx, created.getId(), item.name(), "CREATE", null, neu);
        return new MetadataUpdateResult("TERM", item.name(), true, "CREATE", null, neu, null);
    }

    private void saveAudit(ToolExecutionContext ctx, String entityId, String entityName,
                           String changeType, Map<String, Object> old, Map<String, Object> neu) {
        McpMetadataAuditLogPO po = new McpMetadataAuditLogPO();
        po.setServiceId(ctx.serviceId());
        po.setToolType(getToolType().name());
        po.setEntityType("TERM");
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
