package com.dati.auth.domain.model;

import java.util.Locale;

/**
 * Login names are case-insensitive identifiers: they are always persisted and looked up
 * trimmed and lowercased. Presentation casing belongs to {@code display_name}.
 */
public final class LoginName {

    private LoginName() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

}
