package com.dati.db.analysis;

import jakarta.annotation.Nullable;

public record TableRef(
        @Nullable String schema,
        String name) {

    public static TableRef of(String name) {
        return new TableRef(null, name);
    }

    public static TableRef of(String schema, String name) {
        return new TableRef(schema, name);
    }

    /** Returns "schema.name" or just "name" when schema is null. */
    public String qualifiedName() {
        return schema != null ? schema + "." + name : name;
    }
}
