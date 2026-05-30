package com.dati.common.template;

/** Variable placeholder: {{name}} or {{name:default}}. */
record VarNode(String name, String defaultValue) implements Node {}
