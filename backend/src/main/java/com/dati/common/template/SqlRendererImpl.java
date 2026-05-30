package com.dati.common.template;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
class SqlRendererImpl implements SqlRenderer {

    @Override
    public PreparedSql render(CompiledTemplate compiled, Map<String, Object> params) {
        if (!(compiled instanceof ParsedTemplate pt))
            throw new IllegalArgumentException("CompiledTemplate must be produced by HandlebarsStyleParser");
        StringBuilder sql = new StringBuilder();
        List<ParamBinding> bindings = new ArrayList<>();
        renderNodes(pt.getNodes(), params, sql, bindings);
        return new PreparedSql(sql.toString(), List.copyOf(bindings));
    }

    private void renderNodes(List<Node> nodes, Map<String, Object> params,
                             StringBuilder sql, List<ParamBinding> bindings) {
        for (Node node : nodes) {
            switch (node) {
                case TextNode t -> sql.append(t.text());
                case VarNode v -> renderVar(v, params, sql, bindings);
                case IfNode i -> renderIf(i, params, sql, bindings);
                case WhereNode w -> renderWhere(w, params, sql, bindings);
            }
        }
    }

    private void renderVar(VarNode v, Map<String, Object> params,
                           StringBuilder sql, List<ParamBinding> bindings) {
        Object value = resolveValue(v, params);
        if (isArray(value)) {
            List<?> list = toList(value);
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append('?');
                bindings.add(new ParamBinding(v.name(), list.get(i)));
            }
        } else {
            sql.append('?');
            bindings.add(new ParamBinding(v.name(), value));
        }
    }

    private Object resolveValue(VarNode v, Map<String, Object> params) {
        Object value = params.get(v.name());
        if (value != null) return value;
        return v.defaultValue(); // raw string or null
    }

    private boolean isArray(Object value) {
        return value instanceof List || (value != null && value.getClass().isArray());
    }

    private List<?> toList(Object value) {
        if (value instanceof List<?> l) return l;
        if (value.getClass().isArray()) return Arrays.asList((Object[]) value);
        throw new IllegalStateException("Expected array but got: " + value.getClass());
    }

    private void renderIf(IfNode i, Map<String, Object> params,
                          StringBuilder sql, List<ParamBinding> bindings) {
        if (params.get(i.condition()) != null) {
            renderNodes(i.body(), params, sql, bindings);
        }
    }

    private void renderWhere(WhereNode w, Map<String, Object> params,
                             StringBuilder sql, List<ParamBinding> bindings) {
        StringBuilder body = new StringBuilder();
        List<ParamBinding> bodyBindings = new ArrayList<>();
        renderNodes(w.body(), params, body, bodyBindings);
        String trimmed = body.toString().strip();
        if (trimmed.isEmpty()) return;
        sql.append("WHERE ").append(TextRendererImpl.stripLeadingAndOr(trimmed));
        bindings.addAll(bodyBindings);
    }
}
