package com.dati.mcp.server.resolver;

import com.dati.mcp.domain.model.McpServiceDataScope;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves tool definitions from the active snapshot (not the draft tables).
 * Enabled-only; disabled tools are indistinguishable from unknown tools.
 */
@Component
public class SnapshotToolResolver {

    public Optional<ResolvedTool> resolve(McpServiceSnapshot.SnapshotContent content, String toolName) {
        if (toolName == null) {
            return Optional.empty();
        }
        if (content.getPrebuiltTools() != null) {
            for (McpServiceSnapshot.PrebuiltToolDraft t : content.getPrebuiltTools()) {
                if (t.enabled() && t.toolType() != null && toolName.equals(t.toolType().getToolName())) {
                    return Optional.of(new ResolvedTool(t.toolType(), t.config(), t.toolType().getToolName()));
                }
            }
        }
        if (content.getCustomTools() != null) {
            for (McpServiceSnapshot.CustomToolDraft t : content.getCustomTools()) {
                if (t.enabled() && toolName.equals(t.name())) {
                    return Optional.of(new ResolvedTool(t.toolType(), t.config(), t.name()));
                }
            }
        }
        return Optional.empty();
    }

    /** Builds scope items from snapshot data scopes for ScopeValidator consumption. */
    public List<McpServiceDataScope> buildScopeItems(String serviceId, McpServiceSnapshot.SnapshotContent content) {
        List<McpServiceDataScope> items = new ArrayList<>();
        if (content.getDataScopes() != null) {
            for (McpServiceSnapshot.DataScopeDraft d : content.getDataScopes()) {
                McpServiceDataScope scope = new McpServiceDataScope();
                scope.setServiceId(serviceId);
                scope.setScopeType(d.scopeType());
                scope.setReferenceId(d.referenceId());
                items.add(scope);
            }
        }
        return items;
    }

    public record ResolvedTool(McpToolType toolType, ToolConfig config, String name) {
    }
}
