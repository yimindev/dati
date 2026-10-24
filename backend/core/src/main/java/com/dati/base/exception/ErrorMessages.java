package com.dati.base.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders {@code {0}}-style message templates.
 *
 * <p>Unlike {@link java.text.MessageFormat}, single quotes are ordinary characters and
 * argument values are inserted verbatim. Templates such as
 * {@code "Check the JDBC URL's currentSchema"} therefore render exactly as written.
 */
public final class ErrorMessages {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    private ErrorMessages() {
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
