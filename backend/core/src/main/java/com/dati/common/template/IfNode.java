package com.dati.common.template;

import java.util.List;

/** Conditional block: {{#if var}}...{{/if}}. V1不支持嵌套{{#if}}。 */
record IfNode(String condition, List<Node> body) implements Node {}
