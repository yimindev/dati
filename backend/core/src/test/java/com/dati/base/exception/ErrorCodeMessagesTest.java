package com.dati.base.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCodeMessages tests")
class ErrorCodeMessagesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    @Test
    @DisplayName("every ErrorCode has a Simplified Chinese message")
    void everyErrorCodeHasChineseMessage() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "ErrorCodeMessages", Locale.SIMPLIFIED_CHINESE, NO_FALLBACK);

        for (ErrorCode code : ErrorCode.values()) {
            assertThat(bundle.containsKey("error." + code.getCode()))
                    .as("Missing Chinese message for %s", code.getCode())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Chinese messages keep the same placeholders as the English templates")
    void chineseMessagesKeepPlaceholders() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "ErrorCodeMessages", Locale.SIMPLIFIED_CHINESE, NO_FALLBACK);

        for (ErrorCode code : ErrorCode.values()) {
            String chinese = bundle.getString("error." + code.getCode());
            assertThat(placeholders(chinese))
                    .as("Placeholder mismatch for %s", code.getCode())
                    .isEqualTo(placeholders(code.getTemplate()));
        }
    }

    @Test
    @DisplayName("localized messages render arguments")
    void localizedMessagesRenderArguments() {
        String message = ErrorMessages.formatLocalized(ErrorCode.DS_SQL_ERROR,
                new Object[]{"Table users not found"}, Locale.SIMPLIFIED_CHINESE);

        assertThat(message).isEqualTo("SQL 执行错误：Table users not found");
    }

    @Test
    @DisplayName("missing translations fall back to the English enum template")
    void missingTranslationFallsBackToEnglish() {
        assertThat(ErrorMessages.localizedTemplate(ErrorCode.INTERNAL_ERROR, Locale.ENGLISH))
                .isEqualTo("Internal server error");
        assertThat(ErrorMessages.localizedTemplate(ErrorCode.INTERNAL_ERROR, Locale.FRENCH))
                .isEqualTo("Internal server error");
    }

    @Test
    @DisplayName("Chinese templates preserve single quotes and apostrophes")
    void chineseTemplatePreservesApostrophes() {
        String message = ErrorMessages.formatLocalized(ErrorCode.DS_SCHEMA_DETECTION_FAILED,
                new Object[0], Locale.SIMPLIFIED_CHINESE);

        assertThat(message).contains("currentSchema/search_path");
    }

    private static Set<String> placeholders(String template) {
        Set<String> result = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
