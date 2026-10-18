package com.dati.db.analysis;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Commit;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.RollbackStatement;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.show.ShowIndexStatement;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class SqlAnalyzer {

    /** Matches BEGIN / START TRANSACTION — statements that JSqlParser 5.1 cannot parse. */
    private static final Pattern BEGIN_OR_START_PATTERN =
            Pattern.compile("^\\s*(BEGIN|START\\s+TRANSACTION)", Pattern.CASE_INSENSITIVE);

    private SqlAnalyzer() {}

    /**
     * Analyzes a rendered SQL string (with ? placeholders) and returns one
     * {@link SqlAnalysisResult.StatementAnalysis} per statement, including
     * referenced and created tables.
     *
     * @param sql the SQL to analyze; may be null
     * @return analysis result; empty statements and type=OTHER on blank input or parse failure
     */
    public static SqlAnalysisResult analyze(String sql) {
        if (sql == null || sql.isBlank()) {
            return emptyResult();
        }

        try {
            Statements stmts = CCJSqlParserUtil.parseStatements(sql);
            return analyzeStatements(stmts);
        } catch (JSQLParserException e) {
            try {
                Statement stmt = CCJSqlParserUtil.parse(sql);
                return analyzeSingleStatement(stmt);
            } catch (JSQLParserException ex) {
                if (containsBeginOrStart(sql)) {
                    return transactionPreScan(sql);
                }
                return emptyResult();
            }
        }
    }

    private static boolean containsBeginOrStart(String sql) {
        return BEGIN_OR_START_PATTERN.matcher(sql).find();
    }

    /**
     * Handles SQL containing BEGIN / START TRANSACTION that JSqlParser cannot parse.
     * Splits by semicolon and analyzes each segment individually.
     * <p>
     * Note: {@code split(";")} is a naive split that cannot distinguish semicolons
     * inside string literals (e.g. {@code 'hello;world'}). This is an acceptable
     * limitation because this code path only activates when JSqlParser fails on both
     * {@code parseStatements()} and {@code parse()}, which is rare in practice.
     * regex-detected transaction statements → TRANSACTION,
     * parseable segments → normal type detection + table extraction.
     */
    private static SqlAnalysisResult transactionPreScan(String sql) {
        String[] parts = sql.split(";");
        List<SqlAnalysisResult.StatementAnalysis> statements = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            if (BEGIN_OR_START_PATTERN.matcher(trimmed).find()) {
                statements.add(new SqlAnalysisResult.StatementAnalysis(
                        SqlOperationType.TRANSACTION, Set.of(), Set.of()));
                continue;
            }

            try {
                Statement stmt = CCJSqlParserUtil.parse(trimmed);
                statements.add(analyzeStatement(stmt));
            } catch (JSQLParserException e) {
                statements.add(new SqlAnalysisResult.StatementAnalysis(
                        SqlOperationType.OTHER, Set.of(), Set.of()));
            }
        }

        return new SqlAnalysisResult(statements);
    }

    private static SqlAnalysisResult analyzeStatements(Statements stmts) {
        List<SqlAnalysisResult.StatementAnalysis> statements = stmts.stream()
                .filter(Objects::nonNull)
                .map(SqlAnalyzer::analyzeStatement)
                .toList();
        return new SqlAnalysisResult(statements);
    }

    private static SqlAnalysisResult analyzeSingleStatement(Statement stmt) {
        return new SqlAnalysisResult(List.of(analyzeStatement(stmt)));
    }

    private static SqlAnalysisResult.StatementAnalysis analyzeStatement(Statement stmt) {
        SqlOperationType type = detectOperationType(stmt);

        SchemaAwareTableFinder finder = new SchemaAwareTableFinder();
        finder.extract(stmt);

        Set<TableRef> referenced = new LinkedHashSet<>(finder.getTableRefs());
        Set<TableRef> created = new LinkedHashSet<>();

        if (stmt instanceof CreateTable createTable) {
            TableRef target = toTableRef(createTable.getTable());
            if (target != null) {
                created.add(target);
                referenced.remove(target);
            }
            TableRef likeTable = toTableRef(createTable.getLikeTable());
            if (likeTable != null) {
                referenced.add(likeTable);
            }
        }

        return new SqlAnalysisResult.StatementAnalysis(type, referenced, created);
    }

    private static TableRef toTableRef(Table table) {
        if (table == null) {
            return null;
        }
        String name = table.getUnquotedName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return new TableRef(table.getUnquotedSchemaName(), name);
    }

    private static SqlAnalysisResult emptyResult() {
        return new SqlAnalysisResult(List.of());
    }

    private static SqlOperationType detectOperationType(Statement stmt) {
        if (stmt instanceof Select)             return SqlOperationType.SELECT;
        if (stmt instanceof Insert)             return SqlOperationType.INSERT;
        if (stmt instanceof Update)             return SqlOperationType.UPDATE;
        if (stmt instanceof Delete)             return SqlOperationType.DELETE;
        if (stmt instanceof Merge)              return SqlOperationType.MERGE;
        if (isDDLStatement(stmt))               return SqlOperationType.DDL;
        if (isMetadataStatement(stmt))           return SqlOperationType.METADATA;
        if (stmt instanceof Commit)             return SqlOperationType.TRANSACTION;
        if (stmt instanceof RollbackStatement)  return SqlOperationType.TRANSACTION;
        if (stmt instanceof SetStatement)       return SqlOperationType.SET;
        return SqlOperationType.OTHER;
    }

    private static boolean isMetadataStatement(Statement stmt) {
        return stmt instanceof ShowStatement
                || stmt instanceof ShowTablesStatement
                || stmt instanceof ShowColumnsStatement
                || stmt instanceof ShowIndexStatement
                || stmt instanceof DescribeStatement
                || stmt instanceof ExplainStatement;
    }

    private static boolean isDDLStatement(Statement stmt) {
        return stmt instanceof CreateTable
                || stmt instanceof Alter
                || stmt instanceof Drop
                || stmt instanceof Truncate
                || stmt instanceof CreateIndex
                || stmt instanceof CreateView;
    }

    /**
     * Extends TablesNamesFinder to capture TableRef (schema + name) instead of plain strings.
     */
    private static class SchemaAwareTableFinder extends TablesNamesFinder<Void> {

        private final Set<TableRef> refs = new LinkedHashSet<>();
        private Set<String> cteNames = Set.of();

        void extract(Statement stmt) {
            refs.clear();
            cteNames = collectCteNames(stmt);
            try {
                getTables(stmt);
            } catch (UnsupportedOperationException ignored) {
            }
        }

        @Override
        public <S> Void visit(Table table, S context) {
            String name = table.getUnquotedName();
            String schema = table.getUnquotedSchemaName();
            if (name != null && !cteNames.contains(name)) {
                refs.add(new TableRef(schema, name));
            }
            return null;
        }

        Set<TableRef> getTableRefs() {
            return Set.copyOf(refs);
        }

        private static Set<String> collectCteNames(Statement stmt) {
            if (stmt instanceof Select sel) {
                var withItems = sel.getWithItemsList();
                if (withItems != null && !withItems.isEmpty()) {
                    Set<String> names = new LinkedHashSet<>();
                    for (var item : withItems) {
                        String alias = item.getAliasName();
                        if (alias != null) {
                            names.add(alias);
                        }
                    }
                    return names;
                }
            }
            return Set.of();
        }
    }
}
