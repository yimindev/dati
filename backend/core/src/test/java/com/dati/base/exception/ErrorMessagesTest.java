package com.dati.base.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorMessages tests")
class ErrorMessagesTest {

    @Test
    @DisplayName("keeps single quotes and apostrophes in templates")
    void format_shouldKeepSingleQuotes() {
        String template = "Check the JDBC URL's currentSchema/search_path";

        assertThat(ErrorMessages.format(template)).isEqualTo(template);
        assertThat(ErrorMessages.format(template, "ignored")).isEqualTo(template);
    }

    @Test
    @DisplayName("replaces numbered placeholders in order")
    void format_shouldReplacePlaceholders() {
        assertThat(ErrorMessages.format("Table {0} does not belong to {1}", "orders", "sales"))
                .isEqualTo("Table orders does not belong to sales");
    }

    @Test
    @DisplayName("does not apply MessageFormat number grouping")
    void format_shouldNotGroupNumbers() {
        assertThat(ErrorMessages.format("Limit: {0}", 1000)).isEqualTo("Limit: 1000");
    }

    @Test
    @DisplayName("keeps placeholders without a matching argument")
    void format_shouldKeepMissingPlaceholders() {
        assertThat(ErrorMessages.format("Subject not found: {0}")).isEqualTo("Subject not found: {0}");
    }

    @Test
    @DisplayName("ErrorCode templates with apostrophes render intact")
    void errorCode_shouldRenderApostrophes() {
        DatiException exception = new DatiException(ErrorCode.DS_SCHEMA_DETECTION_FAILED);
        ErrorResponse response = ErrorResponse.of(ErrorCode.DS_SCHEMA_DETECTION_FAILED);

        assertThat(exception.getMessage()).contains("URL's currentSchema");
        assertThat(response.getMessage()).contains("URL's currentSchema");
    }
}
