package com.dati.semantic.domain;

/**
 * Target type of a {@code TermRelation}: a term can only be linked to
 * a table (TABLE-level) or a column (FIELD-level).
 * <p>
 * Distinct from {@link SemanticEntityType} (SUBJECT/TABLE/FIELD/FIELD_VALUE/TERM)
 * which describes indexed semantic entities in Elasticsearch. Using a dedicated
 * enum keeps the relation constraint expressible at the type level: unknown
 * values are rejected by Jackson during deserialization.
 */
public enum TermRelationType {
    TABLE,
    FIELD,
}
