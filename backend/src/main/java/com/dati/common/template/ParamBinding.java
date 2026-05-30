package com.dati.common.template;

/**
 * A single parameter binding: the parameter name (from the template's {{var}})
 * and its value (to be bound via PreparedStatement.setXxx()).
 */
public record ParamBinding(String name, Object value) {}
