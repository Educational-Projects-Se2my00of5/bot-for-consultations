package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для построения клавиатур студента
 */
@Component
public class StudentKeyboardBuilder {

    private static final DateTimeFormatter BUTTON_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter BUTTON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Главное меню студента
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();

        row1.add(new KeyboardButton("🔍 Преподаватели"));
        row2.add(new KeyboardButton("🔔 Подписки на обновления"));
        row2.add(new KeyboardButton("📝 Мои записи"));
        row3.add(new KeyboardButton("❓ Запросить консультацию"));
        row3.add(new KeyboardButton("📋 Просмотреть запросы"));
        row4.add(new KeyboardButton("👤 Профиль"));
        row4.add(new KeyboardButton("Помощь"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Меню для работы с преподавателями
     */
    public ReplyKeyboardMarkup buildTeachersMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("👥 Все преподаватели"));
        row1.add(new KeyboardButton("🔍 Поиск преподавателя"));
        keyboard.add(row1);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура с результатами поиска преподавателей
     */
    public ReplyKeyboardMarkup buildTeacherSearchResults(List<TelegramUser> teachers) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первых 5 преподавателей как кнопки
        int count = 0;
        for (TelegramUser teacher : teachers) {
            if (count >= 5) break;

            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(TeacherNameFormatter.formatFullName(teacher)));
            keyboard.add(row);
            count++;
        }

        // Кнопка поиска
        KeyboardRow searchRow = new KeyboardRow();
        searchRow.add(new KeyboardButton("🔍 Поиск преподавателя"));
        keyboard.add(searchRow);

        // Кнопка "Назад"
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("🔙 К преподавателям"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура со списком консультаций преподавателя
     */
    public ReplyKeyboardMarkup buildTeacherConsultations(
            List<Consultation> consultations,
            boolean isSubscribed) {

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации как кнопки (максимум 5 последних)
        int count = 0;
        for (Consultation consultation : consultations) {
            if (count >= 5) break;
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(String.format("№%d - %s %s",
                    consultation.getId(),
                    consultation.getDate().format(BUTTON_DATE_FORMATTER),
                    consultation.getStartTime().format(BUTTON_TIME_FORMATTER))));
            keyboard.add(row);
            count++;
        }

        // Фильтры
        KeyboardRow filterRow = new KeyboardRow();
        filterRow.add(new KeyboardButton("⏭️ Будущие"));
        filterRow.add(new KeyboardButton("📅 Все"));
        filterRow.add(new KeyboardButton("⏮️ Прошедшие"));
        keyboard.add(filterRow);

        // Подписка/отписка
        KeyboardRow actionRow = new KeyboardRow();
        if (isSubscribed) {
            actionRow.add(new KeyboardButton("🔕 Отписаться"));
        } else {
            actionRow.add(new KeyboardButton("🔔 Подписаться"));
        }
        keyboard.add(actionRow);

        // Навигация
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(new KeyboardButton("🔙 К преподавателям"));
        navRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(navRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для детального просмотра консультации
     *
     * @param consultation консультация для проверки статуса
     * @param isRegistered записан ли студент
     */
    public ReplyKeyboardMarkup buildConsultationDetails(Consultation consultation, boolean isRegistered) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Показываем кнопку записи/отмены только если консультация Open
        if (consultation.getStatus().equals(ConsultationStatus.OPEN)) {
            KeyboardRow actionRow = new KeyboardRow();
            if (isRegistered) {
                actionRow.add(new KeyboardButton("❌ Отменить запись"));
            } else {
                actionRow.add(new KeyboardButton("✅ Записаться"));
            }
            keyboard.add(actionRow);
        }
        // Показываем кнопку записи/отмены только если консультация не Closed
        if (consultation.getStatus().equals(ConsultationStatus.CLOSED)) {
            KeyboardRow actionRow = new KeyboardRow();
            if (isRegistered) {
                actionRow.add(new KeyboardButton("❌ Отменить запись"));
                keyboard.add(actionRow);
            }
        }

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для просмотра списка запросов
     */
    public ReplyKeyboardMarkup buildRequestsList(List<Consultation> requests) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем запросы как кнопки (максимум 10)
        int count = 0;
        for (Consultation request : requests) {
            if (count >= 10) break;
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(String.format("№%d - %s",
                    request.getId(),
                    request.getTitle().length() > 30
                            ? request.getTitle().substring(0, 30) + "..."
                            : request.getTitle())));
            keyboard.add(row);
            count++;
        }

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для детального просмотра запроса
     *
     * @param isRegistered записан ли студент на этот запрос
     */
    public ReplyKeyboardMarkup buildRequestDetails(boolean isRegistered) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow actionRow = new KeyboardRow();
        if (isRegistered) {
            actionRow.add(new KeyboardButton("❌ Отписаться от запроса"));
        } else {
            actionRow.add(new KeyboardButton("✅ Записаться на запрос"));
        }
        keyboard.add(actionRow);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад к списку"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Простая клавиатура с кнопкой "Назад"
     * Используется для отмены операций (например, создание запроса)
     */
    public ReplyKeyboardMarkup buildBackKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }
}
