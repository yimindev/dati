package com.dati.common.template;

/** Variable placeholder: {{name}}, {{name:default}}, or {{{name}}} / {{{name:default}}}. */
record VarNode(String name, String defaultValue, boolean raw) implements Node {}
