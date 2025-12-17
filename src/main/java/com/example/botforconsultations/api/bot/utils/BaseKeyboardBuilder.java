package com.example.botforconsultations.api.bot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс для построения клавиатур.
 * Содержит общие методы и константы для всех KeyboardBuilder'ов.
 */
public abstract class BaseKeyboardBuilder {

    // Форматтеры для дат и времени
    protected static final DateTimeFormatter BUTTON_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    protected static final DateTimeFormatter BUTTON_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Максимальное количество элементов в списке
    protected static final int MAX_LIST_ITEMS = 5;
    protected static final int MAX_REQUESTS_ITEMS = 10;

    /**
     * Создать строку с фильтрами консультаций (Прошедшие, Все, Будущие)
     */
    protected KeyboardRow createFilterRow() {
        return createThreeButtonRow(KeyboardConstants.FILTER_PAST,
                KeyboardConstants.FILTER_ALL,
                KeyboardConstants.FILTER_FUTURE);
    }

    /**
     * Создать строку с одной кнопкой
     */
    protected KeyboardRow createSingleButtonRow(String buttonText) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(buttonText));
        return row;
    }

    /**
     * Создать строку с двумя кнопками
     */
    protected KeyboardRow createTwoButtonRow(String button1Text, String button2Text) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(button1Text));
        row.add(new KeyboardButton(button2Text));
        return row;
    }

    /**
     * Создать строку с тремя кнопками
     */
    protected KeyboardRow createThreeButtonRow(String button1Text, String button2Text, String button3Text) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(button1Text));
        row.add(new KeyboardButton(button2Text));
        row.add(new KeyboardButton(button3Text));
        return row;
    }

    /**
     * Построить финальную клавиатуру из списка строк
     */
    protected ReplyKeyboardMarkup buildKeyboard(List<KeyboardRow> keyboard) {
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Клавиатура для редактирования профиля (общая для всех ролей)
     */
    public ReplyKeyboardMarkup buildProfileKeyboard() {
        return buildProfileKeyboard(false, false, false);
    }

    /**
     * Клавиатура для редактирования профиля с опциональной кнопкой напоминаний
     *
     * @param showReminderButton показывать ли кнопку настройки напоминаний (только для зарегистрированных)
     */
    public ReplyKeyboardMarkup buildProfileKeyboard(boolean showReminderButton) {
        return buildProfileKeyboard(showReminderButton, false, false);
    }

    /**
     * Клавиатура для редактирования профиля с расширенными опциями
     *
     * @param showReminderButton     показывать ли кнопку настройки напоминаний
     * @param showConnectCalendar    показывать ли кнопку подключения Google Calendar
     * @param showDisconnectCalendar показывать ли кнопку отключения Google Calendar
     */
    public ReplyKeyboardMarkup buildProfileKeyboard(boolean showReminderButton,
                                                    boolean showConnectCalendar,
                                                    boolean showDisconnectCalendar) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow(KeyboardConstants.EDIT_FIRST_NAME, KeyboardConstants.EDIT_LAST_NAME));

        if (showReminderButton) {
            keyboard.add(createSingleButtonRow(KeyboardConstants.EDIT_REMINDER_TIME));
        }

        if (showConnectCalendar) {
            keyboard.add(createSingleButtonRow(KeyboardConstants.CONNECT_GOOGLE_CALENDAR));
        }

        if (showDisconnectCalendar) {
            keyboard.add(createSingleButtonRow(KeyboardConstants.DISCONNECT_GOOGLE_CALENDAR));
        }

        keyboard.add(createSingleButtonRow(KeyboardConstants.BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для выбора времени напоминаний
     */
    public ReplyKeyboardMarkup buildReminderTimeKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow("⏱️ 15 минут", "⏱️ 30 минут"));
        keyboard.add(createTwoButtonRow("⏱️ 1 час", "⏱️ 1 день"));
        keyboard.add(createSingleButtonRow(KeyboardConstants.CANCEL));

        return buildKeyboard(keyboard);
    }

    protected String formatDate(LocalDate date) {
        return date != null ? date.format(BUTTON_DATE_FORMATTER) : "";
    }
    
    protected String formatTime(LocalTime time) {
        return time != null ? time.format(BUTTON_TIME_FORMATTER) : "";
    }
}
