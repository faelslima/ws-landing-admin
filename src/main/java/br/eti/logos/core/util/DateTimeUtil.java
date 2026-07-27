package br.eti.logos.core.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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

    /**
     * Converte uma String recebida do frontend em OffsetDateTime.
     * Aceita tanto ISO 8601 completo (ex: "2026-12-31T00:00:00Z") quanto
     * data pura (ex: "2026-12-31"), interpretada como início do dia em UTC.
     * Null-safe e blank-safe: retorna null se o input for null/vazio.
     *
     * @param value String ISO 8601 ou data pura, ou null/vazio
     * @return OffsetDateTime correspondente ou null
     */
    public static OffsetDateTime fromIsoString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
            return LocalDate.parse(value).atStartOfDay().atOffset(ZoneOffset.UTC);
        }
    }
}
