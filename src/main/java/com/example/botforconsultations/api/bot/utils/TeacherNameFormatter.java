package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.TelegramUser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeacherNameFormatter {

    /**
     * Форматирует полное имя преподавателя для отображения в кнопке
     */
    public static String formatFullName(TelegramUser teacher) {
        return String.format("👨‍🏫 %s %s",
                teacher.getFirstName(),
                teacher.getLastName() != null ? teacher.getLastName() : "");
    }

    /**
     * Извлекает имя преподавателя из текста кнопки
     * Формат кнопки: "👨‍🏫 Имя Фамилия"
     */
    public static String[] extractNameParts(String teacherButton) {
        // Убираем эмодзи и лишние пробелы
        String teacherName = teacherButton.replaceFirst("👨‍🏫\\s*", "").trim();
        return teacherName.split("\\s+");
    }
}
