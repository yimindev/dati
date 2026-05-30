package com.dati.common.template;

import java.util.*;

/**
 * Internal implementation of CompiledTemplate. Package-private;
 * renderers access it via package-private {@link #getNodes()}.
 */
class ParsedTemplate implements CompiledTemplate {
    final List<Node> nodes;

    ParsedTemplate(List<Node> nodes) {
        this.nodes = List.copyOf(nodes);
    }

    List<Node> getNodes() {
        return nodes;
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
                default -> {}
            }
        }
    }
}
