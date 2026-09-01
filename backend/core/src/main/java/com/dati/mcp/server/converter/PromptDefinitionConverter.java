package com.dati.mcp.server.converter;

import com.dati.common.template.HandlebarsStyleParser;
import com.dati.common.template.TextRenderer;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import com.dati.mcp.domain.model.PromptParameter;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts snapshot prompt drafts into MCP {@link McpSchema.Prompt} definitions and
 * renders prompts/get results using the existing TEXT template engine.
 */
@Component
public class PromptDefinitionConverter {

    private final HandlebarsStyleParser parser = new HandlebarsStyleParser();
    private final TextRenderer textRenderer = new TextRenderer();

    public List<McpSchema.Prompt> list(McpServiceSnapshot.SnapshotContent content) {
        List<McpSchema.Prompt> prompts = new ArrayList<>();
        if (content.getPrompts() != null) {
            for (McpServiceSnapshot.PromptDraft p : content.getPrompts()) {
                if (!p.enabled()) {
                    continue;
                }
                List<McpSchema.PromptArgument> args = new ArrayList<>();
                if (p.parameters() != null) {
                    for (PromptParameter param : p.parameters()) {
                        args.add(McpSchema.PromptArgument.builder(param.getName())
                            .description(param.getDescription() == null ? "" : param.getDescription())
                            .required(param.isRequired())
                            .build());
                    }
                }
                prompts.add(McpSchema.Prompt.builder(p.name())
                    .description(p.description() == null ? "" : p.description())
                    .arguments(args)
                    .build());
            }
        }
        return prompts;
    }

    public McpSchema.GetPromptResult get(McpServiceSnapshot.SnapshotContent content, String name,
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
        McpSchema.PromptMessage message = McpSchema.PromptMessage.builder(
            McpSchema.Role.USER, McpSchema.TextContent.builder(rendered).build()).build();
        return McpSchema.GetPromptResult.builder(List.of(message)).build();
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
