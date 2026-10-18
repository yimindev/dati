package com.dati.db.analysis;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable SQL analysis result. The per-statement analyses are the stored data;
 * operation type and aggregate table sets are derived views.
 */
public record SqlAnalysisResult(List<StatementAnalysis> statements) {

    public SqlAnalysisResult {
        statements = statements == null ? List.of() : List.copyOf(statements);
    }

    public SqlOperationType type() {
        if (statements.isEmpty()) {
            return SqlOperationType.OTHER;
        }
        return statements.size() > 1
                ? SqlOperationType.MULTI
                : statements.getFirst().type();
    }

    public List<SqlOperationType> statementTypes() {
        return statements.stream()
                .map(StatementAnalysis::type)
                .toList();
    }

    public Set<TableRef> referencedTables() {
        return union(StatementAnalysis::referencedTables);
    }

    public Set<TableRef> createdTables() {
        return union(StatementAnalysis::createdTables);
    }

    public boolean isMulti() {
        return statements.size() > 1;
    }

    private Set<TableRef> union(Function<StatementAnalysis, Set<TableRef>> extractor) {
        Set<TableRef> all = new LinkedHashSet<>();
        for (StatementAnalysis statement : statements) {
            all.addAll(extractor.apply(statement));
        }
        return Set.copyOf(all);
    }

    public record StatementAnalysis(SqlOperationType type,
                                    Set<TableRef> referencedTables,
                                    Set<TableRef> createdTables) {

        public StatementAnalysis {
            referencedTables = referencedTables == null ? Set.of() : Set.copyOf(referencedTables);
            createdTables = createdTables == null ? Set.of() : Set.copyOf(createdTables);
        }
    }
}
