package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.TelegramUser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeacherNameFormatter {

    /**
     * Форматирует полное имя преподавателя для отображения в кнопке
     * Формат: "👨‍🏫 №123 Имя Фамилия"
     */
    public static String formatFullName(TelegramUser teacher) {
        String lastName = teacher.getLastName() != null ? teacher.getLastName() : "";
        return String.format("👨‍🏫 №%d %s %s",
                teacher.getId(),
                teacher.getFirstName(),
                lastName).trim();
    }

    /**
     * Извлекает ID преподавателя из текста кнопки
     * Формат кнопки: "👨‍🏫 №123 Имя Фамилия"
     *
     * @return ID преподавателя или null если не удалось извлечь
     */
    public static Long extractTeacherId(String teacherButton) {
        try {
            // Убираем эмодзи и извлекаем ID
            String cleaned = teacherButton.replaceFirst("👨‍🏫\\s*", "").trim();

            // Ищем ID в формате "№123 "
            if (cleaned.startsWith("№")) {
                // Находим первый пробел после номера
                int spaceIndex = cleaned.indexOf(" ");
                if (spaceIndex > 1) {
                    String idPart = cleaned.substring(1, spaceIndex).trim();
                    return Long.parseLong(idPart);
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
