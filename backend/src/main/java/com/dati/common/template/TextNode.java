package com.dati.common.template;

/** A plain text segment. Rendered as-is in both Text and SQL modes. */
record TextNode(String text) implements Node {}
