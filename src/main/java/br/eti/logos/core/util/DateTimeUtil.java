package br.eti.logos.core.util;

import java.time.OffsetDateTime;

/**
 * Utilitários para conversão de data/hora.
 * DTOs sempre usam String (ISO 8601) para simplicidade de serialização.
 */
public class DateTimeUtil {

    private DateTimeUtil() {
        // Utility class
    }

    /**
     * Converte OffsetDateTime para String ISO 8601.
     * Null-safe: retorna null se input for null.
     *
     * @param dateTime OffsetDateTime da entity
     * @return String ISO 8601 (ex: "2026-06-05T10:30:00Z") ou null
     */
    public static String toIsoString(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
}
