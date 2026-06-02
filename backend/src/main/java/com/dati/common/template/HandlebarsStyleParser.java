package com.dati.common.template;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handlebars-style template parser. Scans for {{ }} delimiters and produces an AST.
 * Context-free: does NOT understand SQL syntax (quotes, comments).
 *
 * <pre>
 * Grammar (V1):
 *   template  := (text | var | ifBlock | whereBlock)*
 *   var       := '{{' name (':' default)? '}}'
 *   ifBlock   := '{{#if ' name '}}' template '{{/if}}'
 *   whereBlock:= '{{#where}}' template '{{/where}}'
 *   escape    := '\{{' → literal '{{'
 * </pre>
 */
@Component
class HandlebarsStyleParser implements TemplateParser {

    private static final Pattern VAR_NAME = Pattern.compile("[A-Za-z0-9_.]+");

    @Override
    public CompiledTemplate parse(String template) throws TemplateParseException {
        List<Node> nodes = new ArrayList<>();
        StringBuilder textBuf = new StringBuilder();
        int i = 0;
        int len = template.length();

        while (i < len) {
            // Escape: \{{  → literal {{
            if (template.charAt(i) == '\\' && i + 2 < len
                    && template.charAt(i + 1) == '{' && template.charAt(i + 2) == '{') {
                textBuf.append("{{");
                i += 3;
                continue;
            }

            // Open tag: {{
            if (template.charAt(i) == '{' && i + 1 < len && template.charAt(i + 1) == '{') {
                flushText(textBuf, nodes);
                i += 2; // past {{

                int close = template.indexOf("}}", i);
                if (close < 0) throw new TemplateParseException("Unclosed '{{' at position " + (i - 2));

                String content = template.substring(i, close).trim();
                i = close + 2; // past }}

                if (content.startsWith("#")) {
                    i = parseBlock(template, content, i, nodes);
                } else if (content.startsWith("/")) {
                    throw new TemplateParseException("Unexpected closing tag '{{" + content + "}}'");
                } else {
                    nodes.add(parseVar(content));
                }
                continue;
            }

            textBuf.append(template.charAt(i));
            i++;
        }

        flushText(textBuf, nodes);
        return new ParsedTemplate(nodes);
    }

    private int parseBlock(String template, String content, int pos, List<Node> nodes) {
        String directive = content.substring(1).trim(); // strip '#'

        if (directive.startsWith("if ")) {
            String cond = directive.substring(3).trim();
            validateVarName(cond, "condition");
            return parseGenericBlock(template, "{{#if " + cond + "}}", "{{/if}}", pos,
                    IfNode::new, cond, true, nodes);
        }
        if (directive.equals("where")) {
            return parseGenericBlock(template, "{{#where}}", "{{/where}}", pos,
                    (cnd, body) -> new WhereNode(body), null, false, nodes);
        }
        throw new TemplateParseException("Unknown block directive: '{{#" + directive + "}}'");
    }

    @FunctionalInterface
    private interface NodeFactory { Node create(String condition, List<Node> body); }

    private int parseGenericBlock(String template, String openTag, String endTag, int pos,
                                   NodeFactory factory, String condition, boolean checkNesting,
                                   List<Node> nodes) {
        int endIdx = findEndTag(template, endTag, pos);
        if (endIdx < 0) throw new TemplateParseException("Unclosed '" + openTag + "' — missing '" + endTag + "'");

        String body = template.substring(pos, endIdx);
        ParsedTemplate sub = (ParsedTemplate) this.parse(body);

        // Check for illegal nesting of same-type blocks in the parsed AST
        if (checkNesting && containsNestedIf(sub.getNodes())) {
            throw new TemplateParseException("Nested '{{#if}}' is not supported");
        }
        if (!checkNesting && containsNestedWhere(sub.getNodes())) {
            throw new TemplateParseException("Nested '{{#where}}' is not supported");
        }

        nodes.add(factory.create(condition, sub.nodes));
        return endIdx + endTag.length();
    }

    private VarNode parseVar(String content) {
        int colon = content.indexOf(':');
        String name = (colon < 0) ? content : content.substring(0, colon);
        validateVarName(name, "variable");
        if (colon < 0) return new VarNode(name, null);
        return new VarNode(name, content.substring(colon + 1));
    }

    private static void validateVarName(String name, String label) {
        if (name.isEmpty() || !VAR_NAME.matcher(name).matches()) {
            throw new TemplateParseException(
                "Invalid " + label + " name: '" + name + "'. Must match [A-Za-z0-9_.]+");
        }
    }

    private static int findEndTag(String template, String endTag, int fromPos) {
        int i = fromPos;
        while (i < template.length()) {
            // Skip \{{ escape sequences (they are literal {{, not tags)
            if (template.charAt(i) == '\\' && i + 2 < template.length()
                    && template.charAt(i + 1) == '{' && template.charAt(i + 2) == '{') {
                i += 3;
                continue;
            }
            if (template.startsWith(endTag, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean containsNestedIf(List<Node> nodes) {
        for (Node node : nodes) {
            switch (node) {
                case IfNode i -> { return true; }
                case WhereNode w -> { if (containsNestedIf(w.body())) return true; }
                default -> {}
            }
        }
        return false;
    }

    private static boolean containsNestedWhere(List<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof WhereNode) return true;
        }
        return false;
    }

    private void flushText(StringBuilder buf, List<Node> nodes) {
        if (!buf.isEmpty()) { nodes.add(new TextNode(buf.toString())); buf.setLength(0); }
    }
}
