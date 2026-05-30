package com.dati.common.template;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Handlebars-style template parser. Scans for {{ }} delimiters and produces an AST.
 * Context-free: does NOT understand SQL syntax (quotes, comments).
 *
 * Grammar (V1):
 *   template  := (text | var | ifBlock | whereBlock)*
 *   var       := '{{' name (':' default)? '}}'
 *   ifBlock   := '{{#if ' name '}}' template '{{/if}}'
 *   whereBlock:= '{{#where}}' template '{{/where}}'
 *   escape    := '\{{' → literal '{{'
 */
@Component
class HandlebarsStyleParser implements TemplateParser {

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
        int endIdx = template.indexOf(endTag, pos);
        if (endIdx < 0) throw new TemplateParseException("Unclosed '" + openTag + "' — missing '" + endTag + "'");

        String body = template.substring(pos, endIdx);

        // Check for illegal nesting of same-type blocks
        if (checkNesting && body.contains("{{#if ")) {
            throw new TemplateParseException("Nested '{{#if}}' is not supported in V1");
        }
        if (!checkNesting && body.contains("{{#where}}")) {
            throw new TemplateParseException("Nested '{{#where}}' is not supported in V1");
        }

        ParsedTemplate sub = (ParsedTemplate) new HandlebarsStyleParser().parse(body);
        nodes.add(factory.create(condition, sub.nodes));
        return endIdx + endTag.length();
    }

    private VarNode parseVar(String content) {
        int colon = content.indexOf(':');
        if (colon < 0) return new VarNode(content, null);
        return new VarNode(content.substring(0, colon), content.substring(colon + 1));
    }

    private void flushText(StringBuilder buf, List<Node> nodes) {
        if (!buf.isEmpty()) { nodes.add(new TextNode(buf.toString())); buf.setLength(0); }
    }
}
