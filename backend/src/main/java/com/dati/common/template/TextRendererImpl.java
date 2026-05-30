package com.dati.common.template;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
class TextRendererImpl implements TextRenderer {

    @Override
    public String render(CompiledTemplate compiled, Map<String, Object> params) {
        if (!(compiled instanceof ParsedTemplate pt))
            throw new IllegalArgumentException("CompiledTemplate must be produced by HandlebarsStyleParser");
        StringBuilder sb = new StringBuilder();
        renderNodes(pt.getNodes(), params, sb);
        return sb.toString();
    }

    private void renderNodes(List<Node> nodes, Map<String, Object> params, StringBuilder sb) {
        for (Node node : nodes) {
            switch (node) {
                case TextNode t -> sb.append(t.text());
                case VarNode v -> renderVar(v, params, sb);
                case IfNode i -> renderIf(i, params, sb);
                case WhereNode w -> renderWhere(w, params, sb);
            }
        }
    }

    private void renderVar(VarNode v, Map<String, Object> params, StringBuilder sb) {
        Object value = params.get(v.name());
        if (value != null) { sb.append(value); }
        else if (v.defaultValue() != null) { sb.append(v.defaultValue()); }
    }

    private void renderIf(IfNode i, Map<String, Object> params, StringBuilder sb) {
        if (params.get(i.condition()) != null) renderNodes(i.body(), params, sb);
    }

    private void renderWhere(WhereNode w, Map<String, Object> params, StringBuilder sb) {
        StringBuilder body = new StringBuilder();
        renderNodes(w.body(), params, body);
        String trimmed = body.toString().strip();
        if (trimmed.isEmpty()) return;
        sb.append("WHERE ").append(stripLeadingAndOr(trimmed));
    }

    static String stripLeadingAndOr(String s) {
        String upper = s.toUpperCase();
        if (upper.startsWith("AND ")) return s.substring(4);
        if (upper.startsWith("OR ")) return s.substring(3);
        if (upper.equals("AND") || upper.equals("OR")) return "";
        return s;
    }
}
