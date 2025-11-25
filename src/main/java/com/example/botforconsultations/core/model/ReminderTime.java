package com.example.botforconsultations.core.model;

import lombok.Getter;

@Getter
public enum ReminderTime {
    MIN_15("15 минут", 15),
    MIN_30("30 минут", 30),
    HOUR_1("1 час", 60),
    DAY_1("1 день", 1440);

    private final String displayName;
    private final int minutesBeforeDeadline;

    ReminderTime(String displayName, int minutesBeforeDeadline) {
        this.displayName = displayName;
        this.minutesBeforeDeadline = minutesBeforeDeadline;
    }

    /**
     * Получить enum по минутам
     */
    public static ReminderTime fromMinutes(int minutes) {
        for (ReminderTime time : values()) {
            if (time.minutesBeforeDeadline == minutes) {
                return time;
            }
        }
        return MIN_30; // По умолчанию
    }
}
