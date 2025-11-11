package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.*;

/**
 * Утилита для построения клавиатур преподавателя.
 * Наследуется от BaseKeyboardBuilder с общими методами.
 */
@Component
public class TeacherKeyboardBuilder extends BaseKeyboardBuilder {

    /**
     * Меню ожидания подтверждения для неактивированных преподавателей
     */
    public ReplyKeyboardMarkup buildWaitingForApprovalMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow(PROFILE));
        return buildKeyboard(keyboard);
    }

    /**
     * Главное меню преподавателя
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow(MY_CONSULTATIONS, CREATE_CONSULTATION));
        keyboard.add(createSingleButtonRow(CONSULTATION_REQUESTS));
        keyboard.add(createTwoButtonRow(PROFILE, HELP));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура со списком консультаций преподавателя
     */
    public ReplyKeyboardMarkup buildConsultationsList(List<Consultation> consultations) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации (максимум 5)
        addConsultationButtons(keyboard, consultations, MAX_LIST_ITEMS);
        
        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра консультации
     */
    public ReplyKeyboardMarkup buildConsultationDetails(Consultation consultation, long registeredCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Кнопки управления в зависимости от статуса
        addConsultationManagementButtons(keyboard, consultation.getStatus());

        // Кнопка "Просмотреть студентов" (если есть записанные)
        if (registeredCount > 0) {
            keyboard.add(createSingleButtonRow(VIEW_STUDENTS));
        }

        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для просмотра консультации другого преподавателя (только чтение)
     */
    public ReplyKeyboardMarkup buildConsultationDetailsReadOnly(long registeredCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Только кнопка "Просмотреть студентов" (если есть записанные)
        if (registeredCount > 0) {
            keyboard.add(createSingleButtonRow(VIEW_STUDENTS));
        }

        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для просмотра списка запросов студентов
     */
    public ReplyKeyboardMarkup buildRequestsList(List<Consultation> requests) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем запросы (максимум 5)
        addConsultationButtons(keyboard, requests, MAX_LIST_ITEMS);
        
        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра запроса студента
     */
    public ReplyKeyboardMarkup buildRequestDetails(int interestedCount) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createSingleButtonRow(ACCEPT_REQUEST));

        // Если есть заинтересованные студенты
        if (interestedCount > 0) {
            keyboard.add(createSingleButtonRow(VIEW_STUDENTS));
        }

        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для подтверждения автозакрытия (Да/Нет)
     */
    public ReplyKeyboardMarkup buildYesNoKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createTwoButtonRow(YES, NO));
        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для выбора параметра редактирования
     */
    public ReplyKeyboardMarkup buildEditMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow(EDIT_TITLE, EDIT_DATE_TIME));
        keyboard.add(createTwoButtonRow(EDIT_CAPACITY, EDIT_AUTO_CLOSE));
        keyboard.add(createSingleButtonRow(BACK));

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
     * Клавиатура с кнопкой "Отмена"
     */
    public ReplyKeyboardMarkup buildCancelKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow(CANCEL));
        return buildKeyboard(keyboard);
    }

    // ========== Приватные вспомогательные методы ==========

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
     * Добавить кнопки управления консультацией в зависимости от статуса
     */
    private void addConsultationManagementButtons(List<KeyboardRow> keyboard, ConsultationStatus status) {
        if (status == ConsultationStatus.OPEN) {
            keyboard.add(createSingleButtonRow(CLOSE_REGISTRATION));
            keyboard.add(createTwoButtonRow(EDIT_CONSULTATION, CANCEL_CONSULTATION));
        } else if (status == ConsultationStatus.CLOSED) {
            keyboard.add(createSingleButtonRow(OPEN_REGISTRATION));
            keyboard.add(createTwoButtonRow(EDIT_CONSULTATION, CANCEL_CONSULTATION));
        }
        // Для CANCELLED статуса не добавляем кнопки управления
    }
}
