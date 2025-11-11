package com.example.botforconsultations.api.bot.utils;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.TodoTask;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.example.botforconsultations.api.bot.utils.KeyboardConstants.*;

/**
 * Утилита для построения клавиатур деканата.
 * Наследуется от BaseKeyboardBuilder с общими методами.
 */
@Component
public class DeaneryKeyboardBuilder extends BaseKeyboardBuilder {

    /**
     * Главное меню деканата
     */
    public ReplyKeyboardMarkup buildMainMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(createTwoButtonRow(TEACHERS_MENU, ALL_TASKS));
        keyboard.add(createTwoButtonRow(PROFILE, HELP));

        return buildKeyboard(keyboard);
    }

    /**
     * Меню ожидания подтверждения для неактивированного деканата
     */
    public ReplyKeyboardMarkup buildWaitingForApprovalMenu() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(createSingleButtonRow(PROFILE));

        return buildKeyboard(keyboard);
    }

    // ========== Работа с преподавателями ==========

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
     * Список найденных преподавателей (первые 5 в кнопках)
     */
    public ReplyKeyboardMarkup buildTeacherListKeyboard(List<TelegramUser> teachers) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первых 5 преподавателей
        addTeacherButtons(keyboard, teachers, MAX_LIST_ITEMS);
        
        keyboard.add(createSingleButtonRow(SEARCH_TEACHER));
        keyboard.add(createSingleButtonRow(BACK_TO_TEACHERS));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура со списком консультаций преподавателя
     * (с кнопками для управления задачами вместо подписки)
     */
    public ReplyKeyboardMarkup buildTeacherConsultations(List<Consultation> consultations) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем консультации 
        addConsultationButtons(keyboard, consultations, MAX_LIST_ITEMS);
        
        // Фильтры консультаций
        keyboard.add(createFilterRow());

        // Действия с задачами преподавателя
        keyboard.add(createTwoButtonRow(CREATE_TASK, TEACHER_TASKS));

        // Навигация
        keyboard.add(createTwoButtonRow(BACK_TO_TEACHERS, BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра консультации
     * (только просмотр студентов, без записи)
     */
    public ReplyKeyboardMarkup buildConsultationDetails() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(createSingleButtonRow(STUDENT_LIST));
        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура для детального просмотра консультации (для деканата с параметром)
     */
    public ReplyKeyboardMarkup buildConsultationDetailsForDeanery(Consultation consultation) {
        // Деканату доступно только просмотр, регистрация не нужна
        return buildConsultationDetails();
    }

    /**
     * Клавиатура после просмотра списка студентов
     */
    public ReplyKeyboardMarkup buildStudentListKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow(BACK));
        return buildKeyboard(keyboard);
    }

    // ========== Работа с задачами ==========

    /**
     * Клавиатура списка всех задач деканата
     */
    public ReplyKeyboardMarkup buildAllTasksList(List<TodoTask> tasks) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первые 5 задач как кнопки
        addTaskButtons(keyboard, tasks, MAX_LIST_ITEMS);

        // Фильтры по дедлайну (как у консультаций)
        keyboard.add(createFilterRow());

        // Фильтры по статусу выполнения
        keyboard.add(createTaskStatusFilterRow());

        // Поиск задачи
        keyboard.add(createSingleButtonRow(SEARCH_TASK));

        // Навигация
        keyboard.add(createSingleButtonRow(BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура списка задач конкретного преподавателя
     */
    public ReplyKeyboardMarkup buildTeacherTasksList(List<TodoTask> tasks) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Добавляем первые 5 задач как кнопки
        addTaskButtons(keyboard, tasks, MAX_LIST_ITEMS);

        // Фильтры по дедлайну
        keyboard.add(createFilterRow());

        // Фильтры по статусу выполнения
        keyboard.add(createTaskStatusFilterRow());

        // Создать новую задачу
        keyboard.add(createSingleButtonRow(CREATE_TASK));

        // Навигация
        keyboard.add(createTwoButtonRow(BACK_TO_TEACHERS, BACK));

        return buildKeyboard(keyboard);
    }

    /**
     * Детальный просмотр задачи
     */
    public ReplyKeyboardMarkup buildTaskDetails(TodoTask task) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Изменение статуса
        if (task.getIsCompleted()) {
            keyboard.add(createSingleButtonRow(MARK_PENDING));
        } else {
            keyboard.add(createSingleButtonRow(MARK_COMPLETED));
        }

        // Управление задачей
        keyboard.add(createTwoButtonRow(EDIT_TASK, DELETE_TASK));

        // Навигация
        keyboard.add(createSingleButtonRow(BACK_TO_LIST));

        return buildKeyboard(keyboard);
    }

    /**
     * Подтверждение удаления задачи
     */
    public ReplyKeyboardMarkup buildDeleteTaskConfirmation() {
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(createTwoButtonRow(CONFIRM_DELETE, CANCEL));

        return buildKeyboard(keyboard);
    }

    /**
     * Клавиатура при создании/редактировании задачи
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
     * Добавить кнопки задач
     */
    private void addTaskButtons(List<KeyboardRow> keyboard, List<TodoTask> tasks, int maxCount) {
        int count = 0;
        for (TodoTask task : tasks) {
            if (count >= maxCount) break;
            String prefix = task.getIsCompleted() ? COMPLETED_PREFIX : PENDING_PREFIX;
            String title = task.getTitle().length() > 25 
                    ? task.getTitle().substring(0, 25) + "..." 
                    : task.getTitle();
            String buttonText = String.format("%s%s%d - %s",
                    prefix,
                    NUMBER_PREFIX,
                    task.getId(),
                    title);
            keyboard.add(createSingleButtonRow(buttonText));
            count++;
        }
    }

    /**
     * Создать строку с фильтрами статуса задач
     */
    private KeyboardRow createTaskStatusFilterRow() {
        return createThreeButtonRow(FILTER_TASK_INCOMPLETE, FILTER_TASK_ALL, FILTER_TASK_COMPLETED);
    }
}
