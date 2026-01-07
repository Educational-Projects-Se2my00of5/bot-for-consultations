package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.ReminderTime;
import com.example.botforconsultations.core.model.TodoTask;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.ACCEPT_REQUEST;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.BACK;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.BACK_TO_LIST;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CANCEL;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CANCEL_CONSULTATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CLOSE_REGISTRATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.CREATE_CONSULTATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_AUTO_CLOSE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_CAPACITY;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_CONSULTATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_DATE_TIME;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_ROLE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.EDIT_TITLE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.FILTER_TASK_ALL;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.FILTER_TASK_COMPLETED;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.FILTER_TASK_INCOMPLETE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.HELP;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.MARK_TASK_COMPLETED;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.MARK_TASK_PENDING;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.MY_CONSULTATIONS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.MY_TASKS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.NO;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.NUMBER_PREFIX;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.OPEN_REGISTRATION;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.PROFILE;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.VIEW_REQUESTS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.VIEW_STUDENTS;
import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.YES;

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
        keyboard.add(createSingleButtonRow(EDIT_ROLE));
        return buildKeyboard(keyboard);
    }

    /**
     * Главное меню преподавателя
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createTwoButtonRow(MY_CONSULTATIONS, CREATE_CONSULTATION));
        keyboard.add(createTwoButtonRow(MY_TASKS, VIEW_REQUESTS));
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

    // ========== Клавиатуры для задач ==========

    /**
     * Клавиатура со списком задач преподавателя
     */
    public ReplyKeyboardMarkup buildTasksList(List<TodoTask> tasks) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем задачи (максимум 5)
        int count = 0;
        for (TodoTask task : tasks) {
            if (count >= MAX_LIST_ITEMS) break;
            String statusIcon = task.getIsCompleted() ? "✅" : "⏳";
            String buttonText = String.format("%s%d - %s %s",
                    NUMBER_PREFIX,
                    task.getId(),
                    statusIcon,
                    task.getTitle());
            keyboard.add(createSingleButtonRow(buttonText));
            count++;
        }

        // Фильтры
        if (!tasks.isEmpty()) {
            keyboard.add(createTwoButtonRow(FILTER_TASK_INCOMPLETE, FILTER_TASK_COMPLETED));
            keyboard.add(createSingleButtonRow(FILTER_TASK_ALL));
        }

        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра задачи
     */
    public ReplyKeyboardMarkup buildTaskDetails(TodoTask task) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Кнопка изменения статуса
        if (task.getIsCompleted()) {
            keyboard.add(createSingleButtonRow(MARK_TASK_PENDING));
        } else {
            keyboard.add(createSingleButtonRow(MARK_TASK_COMPLETED));
        }

        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    // ========== Клавиатуры для напоминаний ==========

    /**
     * Клавиатура главного меню напоминаний (добавить/удалить)
     */
    public ReplyKeyboardMarkup buildReminderTimeMenuKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createTwoButtonRow(KeyboardConstants.ADD_REMINDER_TIME, KeyboardConstants.REMOVE_REMINDER_TIME));
        keyboard.add(createSingleButtonRow(BACK));
        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для добавления времени напоминаний (показывает только не выбранные времена)
     */
    public ReplyKeyboardMarkup buildAddReminderTimeKeyboard(Set<ReminderTime> existingTimes) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        List<String> availableTimes = new ArrayList<>();

        // Добавляем только те времена, которых ещё нет у пользователя
        for (ReminderTime time : ReminderTime.values()) {
            if (!existingTimes.contains(time)) {
                availableTimes.add("⏱️ " + time.getDisplayName());
            }
        }

        // Формируем ряды по 2 кнопки
        for (int i = 0; i < availableTimes.size(); i += 2) {
            if (i + 1 < availableTimes.size()) {
                keyboard.add(createTwoButtonRow(availableTimes.get(i), availableTimes.get(i + 1)));
            } else {
                keyboard.add(createSingleButtonRow(availableTimes.get(i)));
            }
        }

        keyboard.add(createSingleButtonRow(BACK));
        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для удаления времени напоминаний (показывает только выбранные времена)
     */
    public ReplyKeyboardMarkup buildRemoveReminderTimeKeyboard(Set<ReminderTime> existingTimes) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        List<String> selectedTimes = new ArrayList<>();

        // Добавляем только те времена, которые уже выбраны
        for (ReminderTime time : existingTimes) {
            selectedTimes.add("⏱️ " + time.getDisplayName());
        }

        // Формируем ряды по 2 кнопки
        for (int i = 0; i < selectedTimes.size(); i += 2) {
            if (i + 1 < selectedTimes.size()) {
                keyboard.add(createTwoButtonRow(selectedTimes.get(i), selectedTimes.get(i + 1)));
            } else {
                keyboard.add(createSingleButtonRow(selectedTimes.get(i)));
            }
        }

        keyboard.add(createSingleButtonRow(BACK));
        return buildKeyboard(keyboard);
    }
}
