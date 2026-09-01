package com.dati.mcp.server.converter;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpServiceSnapshot;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromptDefinitionConverter tests")
class PromptDefinitionConverterTest {

    private final PromptDefinitionConverter converter = new PromptDefinitionConverter();

    @Test
    @DisplayName("lists enabled prompts with arguments from parameters")
    void listsPrompts() {
        var content = TestFixtures.createTestSnapshotContent();
        List<McpSchema.Prompt> prompts = converter.list(content);
        assertEquals(1, prompts.size());
        assertEquals("analyze_table", prompts.getFirst().name());
        assertEquals(1, prompts.getFirst().arguments().size());
        assertEquals("table", prompts.getFirst().arguments().getFirst().name());
        assertEquals(true, prompts.getFirst().arguments().getFirst().required());
    }

    @Test
    @DisplayName("filters disabled prompts, tolerates null")
    void filtersDisabled() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrompts(List.of(
            TestFixtures.createTestPromptDraft("a", false, "x {{v}}", List.of()),
            TestFixtures.createTestPromptDraft("b", true, "y {{v}}", List.of())));
        List<McpSchema.Prompt> prompts = converter.list(content);
        assertEquals(1, prompts.size());
        assertEquals("b", prompts.getFirst().name());
        assertTrue(converter.list(new McpServiceSnapshot.SnapshotContent()).isEmpty());
    }

    @Test
    @DisplayName("gets prompt with rendered template as user text message")
    void getsPromptRendered() {
        var content = TestFixtures.createTestSnapshotContent();
        McpSchema.GetPromptResult result = converter.get(content, "analyze_table", Map.of("table", "orders"));
        assertEquals(1, result.messages().size());
        assertEquals(McpSchema.Role.USER, result.messages().getFirst().role());
        McpSchema.TextContent text = (McpSchema.TextContent) result.messages().getFirst().content();
        assertEquals("请分析 orders 表的数据。", text.text());
    }

    @Test
    @DisplayName("missing required argument throws IllegalArgumentException")
    void missingRequiredArg() {
        var content = TestFixtures.createTestSnapshotContent();
        assertThrows(IllegalArgumentException.class, () -> converter.get(content, "analyze_table", Map.of()));
    }

    @Test
    @DisplayName("unknown or disabled prompt throws IllegalArgumentException")
    void unknownPrompt() {
        var content = new McpServiceSnapshot.SnapshotContent();
        content.setPrompts(List.of(
            TestFixtures.createTestPromptDraft("off", false, "x", List.of())));
        assertThrows(IllegalArgumentException.class, () -> converter.get(content, "off", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> converter.get(content, "nope", Map.of()));
    }
}
