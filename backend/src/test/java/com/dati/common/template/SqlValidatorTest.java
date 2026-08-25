package com.dati.common.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlValidator unit tests")
class SqlValidatorTest {

    private final HandlebarsStyleParser parser = new HandlebarsStyleParser();
    private final SqlValidator validator = new SqlValidator();

    private List<String> find(String template) {
        return validator.findQuotedVariables(parser.parse(template));
    }

    // ── Violations: variable inside a string literal ──

    @Test
    @DisplayName("Variable directly wrapped in single quotes is flagged")
    void quotedVar_directlyWrapped() {
        assertThat(find("SELECT * FROM genre WHERE name = '{{name}}'")).containsExactly("name");
    }

    @Test
    @DisplayName("Variables separated by spaces inside quotes are flagged")
    void quotedVar_spaceSeparated() {
        assertThat(find("WHERE name = '{{a}} {{b}}'")).containsExactly("a", "b");
    }

    @Test
    @DisplayName("Quoted variables inside {{#if}} block are flagged (all paths checked)")
    void quotedVar_insideIfBlock() {
        String tpl = "SELECT * FROM genre WHERE 1=1 {{#if _user.name}}AND '{{_user.name}}' = '{{_user.name}}'{{/if}}";
        assertThat(find(tpl)).containsExactly("_user.name");
    }

    @Test
    @DisplayName("Quoted variables inside {{#where}} block are flagged")
    void quotedVar_insideWhereBlock() {
        String tpl = "SELECT * FROM orders {{#where}}{{#if status}}AND '{{status}}' = '{{status}}'{{/if}}{{/where}}";
        assertThat(find(tpl)).containsExactly("status");
    }

    @Test
    @DisplayName("Duplicate quoted variable is reported once")
    void quotedVar_deduplicated() {
        assertThat(find("WHERE '{{v}}' = '{{v}}'")).containsExactly("v");
    }

    @Test
    @DisplayName("Quoted variable with default value syntax is flagged")
    void quotedVar_withDefault() {
        assertThat(find("WHERE name = '{{name:unknown}}'")).containsExactly("name");
    }

    @Test
    @DisplayName("Array variable inside quotes is flagged")
    void quotedVar_arrayInQuotes() {
        assertThat(find("WHERE id IN ('{{ids}}')")).containsExactly("ids");
    }

    // ── Violations: variable in comment / dollar-quote (same failure class) ──

    @Test
    @DisplayName("Variable in line comment is flagged")
    void varInLineComment() {
        assertThat(find("SELECT 1 -- {{v}}")).containsExactly("v");
    }

    @Test
    @DisplayName("Variable in block comment is flagged")
    void varInBlockComment() {
        assertThat(find("SELECT 1 /* {{v}} */")).containsExactly("v");
    }

    @Test
    @DisplayName("Variable in dollar-quoted string is flagged")
    void varInDollarQuote() {
        assertThat(find("SELECT $${{v}}$$")).containsExactly("v");
        assertThat(find("SELECT $tag${{v}}$tag$")).containsExactly("v");
    }

    @Test
    @DisplayName("Variable inside E-string with escaped quote is flagged")
    void varInEscapeString() {
        assertThat(find("SELECT E'\\'{{v}}'")).containsExactly("v");
    }

    // ── Pass: valid usages ──

    @Test
    @DisplayName("Variables outside quotes pass")
    void unquotedVars_pass() {
        assertThat(find("SELECT * FROM genre WHERE genreid = {{genre_id}} AND name = {{name}}")).isEmpty();
    }

    @Test
    @DisplayName("Pure string literal without variables passes")
    void pureLiteral_pass() {
        assertThat(find("SELECT * FROM genre WHERE status = 'open'")).isEmpty();
    }

    @Test
    @DisplayName("Escaped quote '' inside literal does not break lexer state")
    void escapedQuote_pass() {
        assertThat(find("SELECT * FROM t WHERE x = 'it''s' AND y = {{y}}")).isEmpty();
    }

    @Test
    @DisplayName("Raw variable inside quotes passes (no placeholder produced)")
    void rawVar_pass() {
        assertThat(find("WHERE name = '{{{v}}}'")).isEmpty();
    }

    @Test
    @DisplayName("Variable inside double quotes passes (matches pgjdbc placeholder counting)")
    void doubleQuotedVar_pass() {
        assertThat(find("SELECT \"{{v}}\" FROM t")).isEmpty();
    }

    @Test
    @DisplayName("Variables inside if/where blocks without quotes pass")
    void blockUnquoted_pass() {
        String tpl = "SELECT * FROM orders {{#where}}{{#if status}}AND status = {{status}}{{/if}}{{/where}}";
        assertThat(find(tpl)).isEmpty();
    }

    @Test
    @DisplayName("Line comment closes at newline; following variable passes")
    void lineCommentNewline_pass() {
        assertThat(find("SELECT 1 -- note\nWHERE x = {{y}}")).isEmpty();
    }

    @Test
    @DisplayName("E-string closed properly; following variable passes")
    void eStringClosed_pass() {
        assertThat(find("SELECT E'a\\'b' WHERE x = {{y}}")).isEmpty();
    }
}
