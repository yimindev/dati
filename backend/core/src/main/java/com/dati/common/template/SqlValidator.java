package com.dati.common.template;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates rendered SQL placeholders against the PostgreSQL lexical rules used by
 * pgjdbc when counting {@code ?} parameters.
 *
 * <p>Every non-raw {@code {{var}}} renders to a {@code ?} placeholder plus one JDBC
 * binding. pgjdbc only counts {@code ?} outside string literals, comments and
 * dollar-quoted regions, so a variable inside such a region always produces a
 * binding-count mismatch at execution time ("column index out of range").
 * This validator detects that statically from the parsed AST, so misuses can be
 * rejected at authoring time instead of failing at runtime.
 */
@Component
public class SqlValidator {

    /** Returns the names of variables whose placeholder would land in a non-parameter region (deduplicated, ordered). */
    public List<String> findQuotedVariables(CompiledTemplate compiled) {
        if (!(compiled instanceof ParsedTemplate(List<Node> nodes)))
            throw new IllegalArgumentException("CompiledTemplate must be produced by HandlebarsStyleParser");

        // Linearize the AST: text nodes as-is, non-raw variables become '?' with a
        // recorded position, if/where blocks are expanded (all branches checked).
        StringBuilder sql = new StringBuilder();
        List<Integer> placeholderPositions = new ArrayList<>();
        List<String> placeholderNames = new ArrayList<>();
        linearize(nodes, sql, placeholderPositions, placeholderNames);

        String text = sql.toString();
        Set<String> violations = new LinkedHashSet<>();
        int pi = 0; // next placeholder position to check
        LexState state = LexState.NORMAL;
        boolean escapeMode = false; // inside E'...' (backslash escapes active)
        String dollarTag = null;

        int i = 0;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);
            char next = i + 1 < len ? text.charAt(i + 1) : '\0';

            // A rendered placeholder at this position: valid only in NORMAL state.
            if (pi < placeholderPositions.size() && i == placeholderPositions.get(pi)) {
                if (state != LexState.NORMAL) {
                    violations.add(placeholderNames.get(pi));
                }
                pi++;
                i++;
                continue;
            }

            switch (state) {
                case NORMAL -> {
                    if (c == '\'') {
                        state = LexState.SINGLE_QUOTE;
                        escapeMode = isEscapeStringPrefix(text, i);
                        i++;
                    } else if (c == '-' && next == '-') {
                        state = LexState.LINE_COMMENT;
                        i += 2;
                    } else if (c == '/' && next == '*') {
                        state = LexState.BLOCK_COMMENT;
                        i += 2;
                    } else if (c == '$' && (dollarTag = matchDollarTag(text, i)) != null) {
                        state = LexState.DOLLAR_QUOTE;
                        i += dollarTag.length();
                    } else {
                        i++;
                    }
                }
                case SINGLE_QUOTE -> {
                    if (escapeMode && c == '\\' && next != '\0') {
                        i += 2; // escaped char pair inside E'...'
                    } else if (c == '\'') {
                        if (next == '\'') {
                            i += 2; // '' escaped quote
                        } else {
                            state = LexState.NORMAL;
                            escapeMode = false;
                            i++;
                        }
                    } else {
                        i++;
                    }
                }
                case LINE_COMMENT -> {
                    if (c == '\n') state = LexState.NORMAL;
                    i++;
                }
                case BLOCK_COMMENT -> {
                    if (c == '*' && next == '/') {
                        state = LexState.NORMAL;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                case DOLLAR_QUOTE -> {
                    if (dollarTag != null && text.startsWith(dollarTag, i)) {
                        int tagLen = dollarTag.length();
                        state = LexState.NORMAL;
                        dollarTag = null;
                        i += tagLen;
                    } else {
                        i++;
                    }
                }
            }
        }
        return new ArrayList<>(violations);
    }

    private void linearize(List<Node> nodes, StringBuilder sql,
                           List<Integer> placeholderPositions, List<String> placeholderNames) {
        for (Node node : nodes) {
            switch (node) {
                case TextNode t -> sql.append(t.text());
                case VarNode v -> {
                    if (v.raw()) {
                        // Raw variables inline their value as text: no placeholder,
                        // no binding, so they cannot cause a count mismatch.
                        continue;
                    }
                    placeholderPositions.add(sql.length());
                    placeholderNames.add(v.name());
                    sql.append('?');
                }
                case IfNode i -> linearize(i.body(), sql, placeholderPositions, placeholderNames);
                case WhereNode w -> linearize(w.body(), sql, placeholderPositions, placeholderNames);
            }
        }
    }

    /** True when the quote at {@code quotePos} is an E'...' string prefix (standalone e/E token). */
    private boolean isEscapeStringPrefix(String text, int quotePos) {
        if (quotePos == 0) return false;
        char prev = text.charAt(quotePos - 1);
        if (prev != 'E' && prev != 'e') return false;
        // The e must not be part of a longer identifier (e.g. name_e'...')
        return quotePos < 2 || !isIdentChar(text.charAt(quotePos - 2));
    }

    /** Matches a dollar-quote opening tag ($$ or $tag$) at {@code pos}; returns the tag, or null. */
    private String matchDollarTag(String text, int pos) {
        int j = pos + 1;
        if (j < text.length() && text.charAt(j) == '$') {
            return "$$";
        }
        if (j < text.length() && (Character.isLetter(text.charAt(j)) || text.charAt(j) == '_')) {
            int k = j;
            while (k < text.length() && (Character.isLetterOrDigit(text.charAt(k)) || text.charAt(k) == '_')) {
                k++;
            }
            if (k < text.length() && text.charAt(k) == '$') {
                return text.substring(pos, k + 1);
            }
        }
        return null;
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private enum LexState { NORMAL, SINGLE_QUOTE, LINE_COMMENT, BLOCK_COMMENT, DOLLAR_QUOTE }
}
