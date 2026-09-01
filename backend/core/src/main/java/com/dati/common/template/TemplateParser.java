package com.dati.common.template;

public interface TemplateParser {
    CompiledTemplate parse(String template) throws TemplateParseException;
}
