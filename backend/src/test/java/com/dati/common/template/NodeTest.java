package com.dati.common.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AST Node 单元测试")
class NodeTest {

    @Test @DisplayName("TextNode 构造和字段访问")
    void testTextNode() {
        TextNode node = new TextNode("hello world");
        assertEquals("hello world", node.text());
    }

    @Test @DisplayName("VarNode 不带默认值")
    void testVarNodeWithoutDefault() {
        VarNode node = new VarNode("status", null);
        assertEquals("status", node.name());
        assertNull(node.defaultValue());
    }

    @Test @DisplayName("VarNode 带默认值")
    void testVarNodeWithDefault() {
        VarNode node = new VarNode("limit", "20");
        assertEquals("limit", node.name());
        assertEquals("20", node.defaultValue());
    }

    @Test @DisplayName("VarNode 带下划线和点号的变量名")
    void testVarNodeComplexName() {
        VarNode node = new VarNode("table.schema.column_name", null);
        assertEquals("table.schema.column_name", node.name());
    }

    @Test @DisplayName("IfNode 构造和字段访问")
    void testIfNode() {
        List<Node> body = List.of(new TextNode("AND status = "), new VarNode("status", null));
        IfNode node = new IfNode("status", body);
        assertEquals("status", node.condition());
        assertEquals(2, node.body().size());
    }

    @Test @DisplayName("WhereNode 构造和字段访问")
    void testWhereNode() {
        IfNode ifNode = new IfNode("status", List.of(new TextNode("AND status = "), new VarNode("status", null)));
        WhereNode node = new WhereNode(List.of(ifNode));
        assertEquals(1, node.body().size());
    }

    @Test @DisplayName("WhereNode 空 body")
    void testWhereNodeEmptyBody() {
        WhereNode node = new WhereNode(List.of());
        assertTrue(node.body().isEmpty());
    }
}
