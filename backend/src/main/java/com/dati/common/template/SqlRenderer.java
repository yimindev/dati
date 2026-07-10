package com.dati.common.template;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class SqlRenderer {

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
        if (v.raw()) {
            if (isArray(value)) {
                throw new TemplateRenderException(
                    "Raw variable '{{{" + v.name() + "}}}' cannot be an array");
            }
            if (value != null) sql.append(value);
            else if (v.defaultValue() != null) sql.append(v.defaultValue());
            return;
        }
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
        return switch (value) {
            case List<?> l -> l;
            case int[] arr -> Arrays.stream(arr).boxed().toList();
            case long[] arr -> Arrays.stream(arr).boxed().toList();
            case double[] arr -> Arrays.stream(arr).boxed().toList();
            case Object[] arr -> Arrays.asList(arr);
            case boolean[] arr -> {
                var list = new ArrayList<Boolean>(arr.length);
                for (boolean b : arr) list.add(b);
                yield list;
            }
            default -> throw new IllegalStateException(
                "Expected array but got: " + value.getClass().getName());
        };
    }

    private static String stripLeadingAndOr(String s) {
        String upper = s.toUpperCase();
        if (upper.startsWith("AND ")) return s.substring(4);
        if (upper.startsWith("OR ")) return s.substring(3);
        if (upper.equals("AND") || upper.equals("OR")) return "";
        return s;
    }

    private void renderIf(IfNode i, Map<String, Object> params,
                          StringBuilder sql, List<ParamBinding> bindings) {
        if (TemplateUtils.isTruthy(params.get(i.condition()))) {
            renderNodes(i.body(), params, sql, bindings);
        }
    }

    private void renderWhere(WhereNode w, Map<String, Object> params,
                             StringBuilder sql, List<ParamBinding> bindings) {
        StringBuilder body = new StringBuilder();
        List<ParamBinding> bodyBindings = new ArrayList<>();
        renderNodes(w.body(), params, body, bodyBindings);
        String trimmed = body.toString().strip();
        // 折叠因 {{#if}} 跳过产生的行间空行（换行+空白+换行 → 单换行）
        trimmed = trimmed.replaceAll("\n\\s*\n", "\n");
        if (trimmed.isEmpty()) return;
        sql.append("WHERE ").append(stripLeadingAndOr(trimmed));
        bindings.addAll(bodyBindings);
    }
}
