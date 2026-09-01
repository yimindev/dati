package com.dati.common.template;

import java.util.*;

/**
 * Internal implementation of CompiledTemplate. Package-private;
 * renderers access it via package-private {@link #nodes ()}.
 */
record ParsedTemplate(List<Node> nodes) implements CompiledTemplate {
    ParsedTemplate(List<Node> nodes) {
        this.nodes = List.copyOf(nodes);
    }

    @Override
    public Set<String> getVariables() {
        Set<String> vars = new LinkedHashSet<>();
        collectVariables(nodes, vars);
        return Collections.unmodifiableSet(vars);
    }

    private void collectVariables(List<Node> nodes, Set<String> vars) {
        for (Node node : nodes) {
            switch (node) {
                case VarNode v -> vars.add(v.name());
                case IfNode i -> {
                    vars.add(i.condition());
                    collectVariables(i.body(), vars);
                }
                case WhereNode w -> collectVariables(w.body(), vars);
                default -> {
                }
            }
        }
    }
}
