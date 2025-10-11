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
     */
    public static String[] extractNameParts(String teacherButton) {
        String teacherName = teacherButton.substring(teacherButton.indexOf(" ") + 1).trim();
        return teacherName.split(" ");
    }
}
