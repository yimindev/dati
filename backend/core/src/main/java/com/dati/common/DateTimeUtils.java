package com.dati.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import jakarta.annotation.Nullable;

/**
 * Date/time parsing utilities for parameter binding.
 */
public final class DateTimeUtils {

    /**
     * Parses a date/time string into {@link LocalDateTime}.
     * Supports common ISO 8601 variants:
     * <ul>
     *   <li>{@code 2026-07-05T14:30:00}</li>
     *   <li>{@code 2026-07-05T14:30:00.000Z}</li>
     *   <li>{@code 2026-07-05T14:30:00+08:00}</li>
     *   <li>{@code 2026-07-05} (date only, treated as 00:00)</li>
     * </ul>
     *
     * @param s the string to parse, may be null
     * @return the parsed date-time, or null if input is null/blank
     * @throws DateTimeParseException if the string cannot be parsed
     */
    @Nullable
    public static LocalDateTime parseDateTime(@Nullable String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        try {
            return OffsetDateTime.parse(t).toLocalDateTime();
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(t);
            } catch (DateTimeParseException e2) {
                return LocalDate.parse(t).atStartOfDay();
            }
        }
    }
}
