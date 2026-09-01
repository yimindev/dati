package com.dati.common.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AST Node unit tests")
class NodeTest {

    @Test @DisplayName("TextNode construction and field access")
    void testTextNode() {
        TextNode node = new TextNode("hello world");
        assertEquals("hello world", node.text());
    }

    @Test @DisplayName("VarNode without default value")
    void testVarNodeWithoutDefault() {
        VarNode node = new VarNode("status", null, false);
        assertEquals("status", node.name());
        assertNull(node.defaultValue());
    }

    @Test @DisplayName("VarNode with default value")
    void testVarNodeWithDefault() {
        VarNode node = new VarNode("limit", "20", false);
        assertEquals("limit", node.name());
        assertEquals("20", node.defaultValue());
    }

    @Test @DisplayName("VarNode with underscore and dot in variable name")
    void testVarNodeComplexName() {
        VarNode node = new VarNode("table.schema.column_name", null, false);
        assertEquals("table.schema.column_name", node.name());
    }

    @Test @DisplayName("IfNode construction and field access")
    void testIfNode() {
        List<Node> body = List.of(new TextNode("AND status = "), new VarNode("status", null, false));
        IfNode node = new IfNode("status", body);
        assertEquals("status", node.condition());
        assertEquals(2, node.body().size());
    }

    @Test @DisplayName("WhereNode construction and field access")
    void testWhereNode() {
        IfNode ifNode = new IfNode("status", List.of(new TextNode("AND status = "), new VarNode("status", null, false)));
        WhereNode node = new WhereNode(List.of(ifNode));
        assertEquals(1, node.body().size());
    }

    @Test @DisplayName("WhereNode with empty body")
    void testWhereNodeEmptyBody() {
        WhereNode node = new WhereNode(List.of());
        assertTrue(node.body().isEmpty());
    }
}
