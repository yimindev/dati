package com.dati.mcp.server.converter;

import com.dati.common.JsonUtils;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig;
import com.dati.mcp.domain.model.ToolParameter;
import com.dati.mcp.domain.service.McpParameterSchemaGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
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
 * Prebuilt schemas are generated from parameter records (single source of truth).
 */
@Slf4j
@Component
public class ToolDefinitionConverter {

    private final McpParameterSchemaGenerator schemaGenerator;

    private static final List<McpToolType> PREBUILT_ORDER = List.of(
        McpToolType.SEARCH_METADATA, McpToolType.GET_TABLE_INFO, McpToolType.LIST_TABLES,
        McpToolType.EXECUTE_SQL, McpToolType.UPDATE_TABLE_INFO,
        McpToolType.UPDATE_COLUMN_INFO, McpToolType.UPSERT_TERM,
        McpToolType.PARAMETERIZED_SQL);

    public ToolDefinitionConverter(McpParameterSchemaGenerator schemaGenerator) {
        this.schemaGenerator = schemaGenerator;
    }

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
        var builder = McpSchema.Tool.builder(type.getToolName(),
                schemaGenerator.generate(type.getParameterType()))
            .description(type.getDescription());
        if (type.getTitle() != null) {
            builder.title(type.getTitle());
        }
        McpSchema.ToolAnnotations annotations = buildAnnotations(type.getAnnotationsJson());
        if (annotations != null) {
            builder.annotations(annotations);
        }
        return builder.build();
    }

    /** Parses the enum-declared annotations JSON; null/blank means no annotations. */
    private McpSchema.ToolAnnotations buildAnnotations(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Map<String, Object> map = JsonUtils.fromJson(json, new TypeReference<>() {});
        return McpSchema.ToolAnnotations.builder()
            .readOnlyHint((Boolean) map.get("readOnlyHint"))
            .destructiveHint((Boolean) map.get("destructiveHint"))
            .idempotentHint((Boolean) map.get("idempotentHint"))
            .openWorldHint((Boolean) map.get("openWorldHint"))
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

    /** Prebuilt: schema generated from parameter record. PARAMETERIZED_SQL custom: generated from ToolParameter list. */
    private Map<String, Object> buildInputSchema(McpServiceSnapshot.CustomToolDraft t) {
        if (t.toolType() == McpToolType.PARAMETERIZED_SQL && t.config() instanceof ToolConfig.ParamSqlConfig cfg) {
            return buildParamSqlSchema(cfg.getParameters());
        }
        if (t.toolType() != null && t.toolType().isPrebuilt()) {
            return schemaGenerator.generate(t.toolType().getParameterType());
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
}
