package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.*;

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
            String buttonText = String.format("%s%d - %s %s",
                    NUMBER_PREFIX,
                    consultation.getId(),
                    consultation.getDate().format(BUTTON_DATE_FORMATTER),
                    consultation.getStartTime().format(BUTTON_TIME_FORMATTER));
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
