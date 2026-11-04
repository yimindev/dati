package com.dati.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdbcConnector logging safety")
class JdbcConnectorTest {

    @Test
    @DisplayName("toString masks the password and credentials embedded in the URL")
    void toStringShouldNotLeakPassword() {
        JdbcConnector connector = new JdbcConnector(
                "jdbc:mysql://root:url-secret@db.internal:3306/shop?password=query-secret", "root", "field-secret");

        assertThat(connector.toString())
                .doesNotContain("field-secret", "url-secret", "query-secret")
                .contains("db.internal:3306/shop");
    }
}
