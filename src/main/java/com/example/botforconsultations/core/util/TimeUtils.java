package com.example.botforconsultations.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Утилитный класс для работы со временем в проекте.
 * Все временные операции должны использовать часовой пояс Томска (UTC+7).
 */
public final class TimeUtils {

    /**
     * Часовой пояс Томска (Asia/Tomsk, UTC+7)
     */
    public static final ZoneId TOMSK_ZONE = ZoneId.of("Asia/Tomsk");

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
}
