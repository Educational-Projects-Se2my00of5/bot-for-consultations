package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.ALL_TEACHERS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.BACK;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.BACK_TO_LIST;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.BACK_TO_TEACHERS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CANCEL;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CANCEL_REGISTRATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.HELP;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.MY_REGISTRATIONS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.NUMBER_PREFIX;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.PROFILE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.REGISTER;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.REGISTER_FOR_REQUEST;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.REQUEST_CONSULTATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.SEARCH_TEACHER;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.SUBSCRIBE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.SUBSCRIPTIONS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.TEACHERS_MENU;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.UNREGISTER_FROM_REQUEST;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.UNSUBSCRIBE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.VIEW_REQUESTS;

/**
 * Утилита для построения клавиатур студента.
 * Наследуется от BaseKeyboardBuilder с общими методами.
 */
@Component
public class StudentKeyboardBuilder extends BaseKeyboardBuilder {

    /**
     * Главное меню студента
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createSingleButtonRow(TEACHERS_MENU));
        keyboard.add(createTwoButtonRow(SUBSCRIPTIONS, MY_REGISTRATIONS));
        keyboard.add(createTwoButtonRow(REQUEST_CONSULTATION, VIEW_REQUESTS));
        keyboard.add(createTwoButtonRow(PROFILE, HELP));

        return buildKeyboard(keyboard);
    }

    /**
     * Меню для работы с преподавателями
     */
    public ReplyKeyboardMarkup buildTeachersMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow(ALL_TEACHERS, SEARCH_TEACHER));
        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура с результатами поиска преподавателей
     */
    public ReplyKeyboardMarkup buildTeacherSearchResults(List<TelegramUser> teachers) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первых 5 преподавателей как кнопки
        addTeacherButtons(keyboard, teachers, MAX_LIST_ITEMS);

        keyboard.add(createSingleButtonRow(SEARCH_TEACHER));
        keyboard.add(createSingleButtonRow(BACK_TO_TEACHERS));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура со списком консультаций преподавателя
     */
    public ReplyKeyboardMarkup buildTeacherConsultations(
            List<Consultation> consultations,
            boolean isSubscribed) {

        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации (максимум 5)
        addConsultationButtons(keyboard, consultations, MAX_LIST_ITEMS);

        // Фильтры
        keyboard.add(createFilterRow());

        // Подписка/отписка
        keyboard.add(createSubscriptionRow(isSubscribed));

        // Навигация
        keyboard.add(createTwoButtonRow(BACK_TO_TEACHERS, BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра консультации
     */
    public ReplyKeyboardMarkup buildConsultationDetails(Consultation consultation, boolean isRegistered) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Кнопка записи/отмены зависит от статуса консультации
        addRegistrationButtons(keyboard, consultation, isRegistered);

        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для просмотра списка запросов
     */
    public ReplyKeyboardMarkup buildRequestsList(List<Consultation> requests) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем запросы (максимум 10)
        addConsultationButtons(keyboard, requests, MAX_REQUESTS_ITEMS);

        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра запроса
     */
    public ReplyKeyboardMarkup buildRequestDetails(boolean isRegistered) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createRequestActionRow(isRegistered));
        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Простая клавиатура с кнопкой "Назад"
     */
    public ReplyKeyboardMarkup buildBackKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow(BACK));
        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура с кнопкой "Отмена" для прерывания ввода
     */
    public ReplyKeyboardMarkup buildCancelKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow(CANCEL));
        return buildKeyboard(keyboard);
    }

    // ========== Приватные вспомогательные методы ==========

    /**
     * Добавить кнопки преподавателей
     */
    private void addTeacherButtons(List<KeyboardRow> keyboard, List<TelegramUser> teachers, int maxCount) {
        int count = 0;
        for (TelegramUser teacher : teachers) {
            if (count >= maxCount) break;
            keyboard.add(createSingleButtonRow(TeacherNameFormatter.formatFullName(teacher)));
            count++;
        }
    }

    /**
     * Добавить кнопки консультаций
     */
    private void addConsultationButtons(List<KeyboardRow> keyboard, List<Consultation> consultations, int maxCount) {
        int count = 0;
        for (Consultation consultation : consultations) {
            if (count >= maxCount) break;
            String buttonText;
            if (consultation.getDate() != null) {
                buttonText = String.format("%s%d - %s %s",
                        NUMBER_PREFIX,
                        consultation.getId(),
                        formatDate(consultation.getDate()),
                        formatTime(consultation.getStartTime())
                );
            } else {
                buttonText = String.format("%s%d - %s",
                        NUMBER_PREFIX,
                        consultation.getId(),
                        consultation.getTitle()
                );
            }
            keyboard.add(createSingleButtonRow(buttonText));
            count++;
        }
    }

    /**
     * Создать строку с кнопкой подписки/отписки
     */
    private KeyboardRow createSubscriptionRow(boolean isSubscribed) {
        return createSingleButtonRow(isSubscribed ? UNSUBSCRIBE : SUBSCRIBE);
    }

    /**
     * Создать строку с кнопкой действия для запроса
     */
    private KeyboardRow createRequestActionRow(boolean isRegistered) {
        return createSingleButtonRow(isRegistered ? UNREGISTER_FROM_REQUEST : REGISTER_FOR_REQUEST);
    }

    /**
     * Добавить кнопки записи/отмены в зависимости от статуса консультации
     */
    private void addRegistrationButtons(List<KeyboardRow> keyboard, Consultation consultation, boolean isRegistered) {
        ConsultationStatus status = consultation.getStatus();

        // Для открытых консультаций - полный функционал
        if (status == ConsultationStatus.OPEN) {
            String buttonText = isRegistered ? CANCEL_REGISTRATION : REGISTER;
            keyboard.add(createSingleButtonRow(buttonText));
        }
        // Для закрытых консультаций - только отмена записи, если записан
        else if (status == ConsultationStatus.CLOSED && isRegistered) {
            keyboard.add(createSingleButtonRow(CANCEL_REGISTRATION));
        }
    }
}
