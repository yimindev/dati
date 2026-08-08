package com.dati.mcp.server.converter;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts snapshot tool drafts into MCP protocol {@link McpSchema.Tool} definitions.
 * Deterministic order: prebuilt fixed order, then custom tools by name.
 * Custom tools whose name collides with prebuilt or earlier custom tools are skipped (WARN logged).
 */
@Slf4j
@Component
public class ToolDefinitionConverter {

    private static final List<McpToolType> PREBUILT_ORDER = List.of(
        McpToolType.SEARCH_METADATA, McpToolType.GET_TABLE_INFO,
        McpToolType.EXECUTE_SQL, McpToolType.PARAMETERIZED_SQL);

    public List<McpSchema.Tool> convert(McpServiceSnapshot.SnapshotContent content) {
        List<McpSchema.Tool> tools = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        if (content.getPrebuiltTools() != null) {
            for (McpToolType type : PREBUILT_ORDER) {
                content.getPrebuiltTools().stream()
                    .filter(t -> t.toolType() == type && t.enabled())
                    .findFirst()
                    .ifPresent(t -> {
                        tools.add(buildPrebuilt(type));
                        usedNames.add(type.getToolName());
                    });
            }
        }
        if (content.getCustomTools() != null) {
            content.getCustomTools().stream()
                .filter(McpServiceSnapshot.CustomToolDraft::enabled)
                .sorted(Comparator.comparing(McpServiceSnapshot.CustomToolDraft::name))
                .filter(t -> usedNames.add(t.name()))
                .forEach(t -> tools.add(buildCustom(t)));
        }
        return tools;
    }

    private McpSchema.Tool buildPrebuilt(McpToolType type) {
        return McpSchema.Tool.builder(type.getToolName(), parseSchema(type.getInputSchema()))
            .description(type.getDescription())
            .build();
    }

    private McpSchema.Tool buildCustom(McpServiceSnapshot.CustomToolDraft t) {
        var builder = McpSchema.Tool.builder(t.name(), buildInputSchema(t))
            .description(t.description());
        if (t.title() != null && !t.title().isBlank()) {
            builder.title(t.title());
        }
        return builder.build();
    }

    /** Prebuilt: parse enum JSON Schema. PARAMETERIZED_SQL custom: generate from ToolParameter list. */
    private Map<String, Object> buildInputSchema(McpServiceSnapshot.CustomToolDraft t) {
        if (t.toolType() == McpToolType.PARAMETERIZED_SQL && t.config() instanceof ToolConfig.ParamSqlConfig cfg) {
            return buildParamSqlSchema(cfg.getParameters());
        }
        if (t.toolType() != null && t.toolType().getInputSchema() != null) {
            return parseSchema(t.toolType().getInputSchema());
        }
        Map<String, Object> empty = new HashMap<>();
        empty.put("type", "object");
        empty.put("additionalProperties", false);
        return empty;
    }

    private Map<String, Object> buildParamSqlSchema(List<ToolParameter> parameters) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        if (parameters != null) {
            for (ToolParameter p : parameters) {
                Map<String, Object> prop = new HashMap<>();
                prop.put("type", jsonSchemaType(p.getType()));
                if (p.getDescription() != null) {
                    prop.put("description", p.getDescription());
                }
                if (p.getDefaultValue() != null) {
                    prop.put("default", p.getDefaultValue());
                }
                properties.put(p.getName(), prop);
                if (p.isRequired()) {
                    required.add(p.getName());
                }
            }
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private String jsonSchemaType(String toolParamType) {
        return switch (toolParamType == null ? "" : toolParamType) {
            case "Number" -> "number";
            case "Boolean" -> "boolean";
            case "Array" -> "array";
            default -> "string";   // String / DateTime / unknown
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSchema(String json) {
        if (json == null || json.isBlank()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("type", "object");
            return empty;
        }
        return JsonUtils.fromJson(json, Map.class);
    }
}
