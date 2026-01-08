package com.example.botforconsultations.core.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Утилитный класс для работы со временем в проекте.
 * Все временные операции должны использовать часовой пояс Томска (UTC+7).
 */
public final class TimeUtils {

    /**
     * Часовой пояс Томска (Asia/Tomsk, UTC+7)
     */
    public static final ZoneId TOMSK_ZONE = ZoneId.of("Asia/Tomsk");

    /**
     * Форматтеры для парсинга времени (с поддержкой формата без ведущего нуля)
     */
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("HH:mm"),  // 08:30
            DateTimeFormatter.ofPattern("H:mm")    // 8:30
    };

    /**
     * Форматтеры для парсинга даты-времени (с поддержкой формата без ведущего нуля)
     */
    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),  // 08.01.2026 08:30
            DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm"),   // 08.01.2026 8:30
            DateTimeFormatter.ofPattern("d.MM.yyyy HH:mm"),   // 8.01.2026 08:30
            DateTimeFormatter.ofPattern("d.MM.yyyy H:mm"),    // 8.01.2026 8:30
            DateTimeFormatter.ofPattern("dd.M.yyyy HH:mm"),   // 08.1.2026 08:30
            DateTimeFormatter.ofPattern("dd.M.yyyy H:mm"),    // 08.1.2026 8:30
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm"),    // 8.1.2026 08:30
            DateTimeFormatter.ofPattern("d.M.yyyy H:mm")      // 8.1.2026 8:30
    };

    private TimeUtils() {
        // Утилитный класс, экземпляры не создаются
    }

    /**
     * Получить текущее время в часовом поясе Томска
     *
     * @return текущее LocalDateTime в часовом поясе Томска
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(TOMSK_ZONE);
    }

    /**
     * Получить текущее время с информацией о часовом поясе
     *
     * @return текущее ZonedDateTime в часовом поясе Томска
     */
    public static ZonedDateTime zonedNow() {
        return ZonedDateTime.now(TOMSK_ZONE);
    }

    /**
     * Конвертировать LocalDateTime в ZonedDateTime с часовым поясом Томска
     *
     * @param dateTime локальное время
     * @return ZonedDateTime в часовом поясе Томска
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(TOMSK_ZONE);
    }

    /**
     * Проверить, находится ли время в прошлом относительно текущего момента
     *
     * @param dateTime время для проверки
     * @return true, если время в прошлом
     */
    public static boolean isPast(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.isBefore(now());
    }

    /**
     * Проверить, находится ли время в будущем относительно текущего момента
     *
     * @param dateTime время для проверки
     * @return true, если время в будущем
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.isAfter(now());
    }

    /**
     * Проверить, истёк ли срок (время в прошлом)
     *
     * @param expiresAt время истечения
     * @return true, если срок истёк
     */
    public static boolean isExpired(LocalDateTime expiresAt) {
        return isPast(expiresAt);
    }

    /**
     * Парсинг времени с поддержкой разных форматов.
     * Поддерживает форматы: HH:mm (08:30) и H:mm (8:30)
     *
     * @param timeStr строка со временем
     * @return LocalTime или null если парсинг не удался
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }
        
        String trimmed = timeStr.trim();
        
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        
        return null;
    }

    /**
     * Парсинг даты и времени с поддержкой разных форматов.
     * Поддерживает форматы с ведущими нулями и без.
     *
     * @param dateTimeStr строка с датой и временем (dd.MM.yyyy HH:mm)
     * @return LocalDateTime или null если парсинг не удался
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        
        String trimmed = dateTimeStr.trim();
        
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        
        return null;
    }
}
