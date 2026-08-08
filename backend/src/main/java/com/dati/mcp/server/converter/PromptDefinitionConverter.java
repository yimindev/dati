package com.dati.mcp.server.converter;

import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.TextRenderer;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.PromptParameter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts snapshot prompt drafts into MCP Prompt definitions and renders
 * prompts/get results using the existing TEXT template engine.
 */
@Component
public class PromptDefinitionConverter {

    private final HandlebarsStyleParser parser = new HandlebarsStyleParser();
    private final TextRenderer textRenderer = new TextRenderer();

    public List<Map<String, Object>> list(McpServiceSnapshot.SnapshotContent content) {
        List<Map<String, Object>> prompts = new ArrayList<>();
        if (content.getPrompts() != null) {
            for (McpServiceSnapshot.PromptDraft p : content.getPrompts()) {
                if (!p.enabled()) {
                    continue;
                }
                Map<String, Object> def = new HashMap<>();
                def.put("name", p.name());
                def.put("description", p.description() == null ? "" : p.description());
                List<Map<String, Object>> args = new ArrayList<>();
                if (p.parameters() != null) {
                    for (PromptParameter param : p.parameters()) {
                        Map<String, Object> arg = new HashMap<>();
                        arg.put("name", param.getName());
                        arg.put("description", param.getDescription() == null ? "" : param.getDescription());
                        arg.put("required", param.isRequired());
                        args.add(arg);
                    }
                }
                def.put("arguments", args);
                prompts.add(def);
            }
        }
        return prompts;
    }

    public Map<String, Object> get(McpServiceSnapshot.SnapshotContent content, String name,
                                   Map<String, Object> arguments) {
        McpServiceSnapshot.PromptDraft prompt = findEnabled(content, name);
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown prompt: " + name);
        }
        Map<String, Object> values = arguments == null ? Map.of() : new HashMap<>(arguments);
        if (prompt.parameters() != null) {
            for (PromptParameter param : prompt.parameters()) {
                if (param.isRequired() && !values.containsKey(param.getName())) {
                    throw new IllegalArgumentException("Missing required argument: " + param.getName());
                }
            }
        }
        String rendered = textRenderer.render(parser.parse(prompt.content()), values);
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", Map.of("type", "text", "text", rendered));
        Map<String, Object> result = new HashMap<>();
        result.put("messages", List.of(message));
        return result;
    }

    private McpServiceSnapshot.PromptDraft findEnabled(McpServiceSnapshot.SnapshotContent content, String name) {
        if (content.getPrompts() == null) {
            return null;
        }
        return content.getPrompts().stream()
            .filter(p -> p.enabled() && name.equals(p.name()))
            .findFirst().orElse(null);
    }
}
