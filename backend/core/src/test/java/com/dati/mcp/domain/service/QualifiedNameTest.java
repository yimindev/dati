package com.dati.mcp.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualifiedNameTest {

    @Test
    @DisplayName("joins all non-blank parts with dots")
    void joinsAllParts() {
        assertThat(QualifiedName.of("public", "orders", "status")).isEqualTo("public.orders.status");
        assertThat(QualifiedName.of("销售", "退货单")).isEqualTo("销售.退货单");
    }

    @Test
    @DisplayName("skips blank parts so a missing schema degrades gracefully")
    void skipsBlankParts() {
        assertThat(QualifiedName.of(null, "orders", "status")).isEqualTo("orders.status");
        assertThat(QualifiedName.of("", "orders")).isEqualTo("orders");
        assertThat(QualifiedName.of("  ", "orders")).isEqualTo("orders");
    }

    @Test
    @DisplayName("returns empty string when no parts remain")
    void emptyWhenNoParts() {
        assertThat(QualifiedName.of(null, "", " ")).isEmpty();
    }
}
