package com.dati.base.exception;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code {0}}-style message templates.
 *
 * <p>Unlike {@link java.text.MessageFormat}, single quotes are ordinary characters and
 * argument values are inserted verbatim. Templates such as
 * {@code "Check the JDBC URL's currentSchema"} therefore render exactly as written.
 *
 * <p>Localized templates are loaded from {@code ErrorCodeMessages_<locale>.properties};
 * a missing key falls back to the English template defined on {@link ErrorCode}.
 */
public final class ErrorMessages {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");
    private static final String BUNDLE_NAME = "ErrorCodeMessages";
    private static final String KEY_PREFIX = "error.";
    private static final ResourceBundle.Control BUNDLE_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private ErrorMessages() {
    }

    /**
     * Returns the localized template for an error code. The JVM default locale is never used
     * as a fallback: an English request must not receive Chinese text on a Chinese machine.
     */
    public static String localizedTemplate(ErrorCode errorCode, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, BUNDLE_CONTROL);
            String key = KEY_PREFIX + errorCode.getCode();
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (MissingResourceException ignored) {
            // No bundle for this locale; use the English template below.
        }
        return errorCode.getTemplate();
    }

    public static String formatLocalized(ErrorCode errorCode, Object[] args, Locale locale) {
        return format(localizedTemplate(errorCode, locale), args);
    }

    /**
     * Replaces {@code {n}} placeholders with the corresponding argument. Placeholders without
     * a matching argument are kept unchanged so partially resolved templates stay recognizable.
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return null;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String replacement = index < args.length ? String.valueOf(args[index]) : matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
