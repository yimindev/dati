package com.dati.mcp.domain.service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds fully-qualified entity names for metadata update results, e.g.
 * {@code public.orders.status} for a column or {@code 销售.退货单} for a term.
 * Blank/null parts are skipped so a missing schema degrades gracefully.
 */
public final class QualifiedName {

    private QualifiedName() {
    }

    /** Joins non-blank parts with "." — the single naming convention for update result entities. */
    public static String of(String... parts) {
        return Arrays.stream(parts)
            .filter(p -> p != null && !p.isBlank())
            .collect(Collectors.joining("."));
    }
}
