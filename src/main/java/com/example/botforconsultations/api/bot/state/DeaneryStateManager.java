package com.example.botforconsultations.api.bot.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер состояний деканата.
 * Наследуется от BaseStateManager с общей логикой.
 */
@Component
@RequiredArgsConstructor
public class DeaneryStateManager extends BaseStateManager<DeaneryStateManager.DeaneryState> {

    public enum DeaneryState {
        DEFAULT,                           // Обычное состояние (главное меню)
        WAITING_FOR_TEACHER_NAME,          // Ожидание ввода имени преподавателя для поиска
        VIEWING_TEACHER_CONSULTATIONS,     // Просмотр консультаций преподавателя
        VIEWING_CONSULTATION_DETAILS,      // Просмотр деталей конкретной консультации
        VIEWING_TEACHER_TASKS,             // Просмотр задач преподавателя
        VIEWING_TASK_DETAILS,              // Просмотр деталей конкретной задачи
        VIEWING_ALL_TASKS,                 // Просмотр всех задач
        CREATING_TODO_TITLE,               // Ввод названия задачи
        CREATING_TODO_DESCRIPTION,         // Ввод описания задачи
        CREATING_TODO_DEADLINE,            // Ввод дедлайна задачи
        CONFIRMING_DELETE_TASK,            // Подтверждение удаления задачи
        EDITING_TODO_TITLE,                // Редактирование названия задачи
        EDITING_TODO_DESCRIPTION,          // Редактирование описания задачи
        EDITING_TODO_DEADLINE,             // Редактирование дедлайна задачи

        // Состояния редактирования профиля для неподтвержденных пользователей
        WAITING_APPROVAL_EDITING_FIRST_NAME,  // Ожидание ввода нового имени (неподтвержденный)
        WAITING_APPROVAL_EDITING_LAST_NAME,   // Ожидание ввода новой фамилии (неподтвержденный)
        WAITING_APPROVAL_ROLE_SELECTION,      // Ожидание выбора роли (неподтвержденный)
        WAITING_DELETE_CONFIRMATION,          // Ожидание подтверждения удаления аккаунта

        // Состояния редактирования профиля для подтвержденных пользователей
        EDITING_PROFILE_FIRST_NAME,        // Ожидание ввода нового имени (подтвержденный)
        EDITING_PROFILE_LAST_NAME          // Ожидание ввода новой фамилии (подтвержденный)
    }

    /**
     * Data class для временных данных создания задачи (вариант 3)
     */
    @Getter
    @Setter
    public static class TodoCreationData {
        private Long teacherId;
        private String title;
        private String description;
    }

    // Специфичные для деканата данные (композиция)
    private final EntityIdStorage teacherIds = new EntityIdStorage();
    private final EntityIdStorage consultationIds = new EntityIdStorage();
    private final EntityIdStorage taskIds = new EntityIdStorage();
    private final Map<Long, String> consultationFilter = new HashMap<>();
    private final Map<Long, String> taskStatusFilter = new HashMap<>();
    private final Map<Long, String> taskDeadlineFilter = new HashMap<>();
    private final Map<Long, TodoCreationData> todoCreationDataMap = new HashMap<>();
    private final Map<Long, DeaneryState> previousState = new HashMap<>();

    @Override
    protected DeaneryState getDefaultState() {
        return DeaneryState.DEFAULT;
    }

    @Override
    protected void clearSpecificData(Long chatId) {
        teacherIds.clear(chatId);
        consultationIds.clear(chatId);
        taskIds.clear(chatId);
        consultationFilter.remove(chatId);
        taskStatusFilter.remove(chatId);
        taskDeadlineFilter.remove(chatId);
        todoCreationDataMap.remove(chatId);
        previousState.remove(chatId);
    }

    // ========== Специфичные методы для деканата ==========

    /**
     * Установить текущего преподавателя
     */
    public void setCurrentTeacher(Long chatId, Long teacherId) {
        teacherIds.set(chatId, teacherId);
    }

    /**
     * Получить ID текущего преподавателя
     */
    public Long getCurrentTeacher(Long chatId) {
        return teacherIds.get(chatId);
    }

    /**
     * Установить фильтр консультаций
     */
    public void setFilter(Long chatId, String filter) {
        consultationFilter.put(chatId, filter);
    }

    /**
     * Получить фильтр консультаций
     */
    public String getFilter(Long chatId) {
        return consultationFilter.getOrDefault(chatId, "future");
    }

    /**
     * Очистить ID текущего преподавателя
     */
    public void clearCurrentTeacher(Long chatId) {
        teacherIds.clear(chatId);
        consultationFilter.remove(chatId);
    }

    /**
     * Установить текущую консультацию
     */
    public void setCurrentConsultation(Long chatId, Long consultationId) {
        consultationIds.set(chatId, consultationId);
    }

    /**
     * Получить ID текущей консультации
     */
    public Long getCurrentConsultation(Long chatId) {
        return consultationIds.get(chatId);
    }

    /**
     * Очистить ID текущей консультации
     */
    public void clearCurrentConsultation(Long chatId) {
        consultationIds.clear(chatId);
    }

    /**
     * Установить текущую задачу
     */
    public void setCurrentTask(Long chatId, Long taskId) {
        taskIds.set(chatId, taskId);
    }

    /**
     * Получить ID текущей задачи
     */
    public Long getCurrentTask(Long chatId) {
        return taskIds.get(chatId);
    }

    /**
     * Очистить ID текущей задачи
     */
    public void clearCurrentTask(Long chatId) {
        taskIds.clear(chatId);
    }

    /**
     * Получить данные создания задачи (автоматически создаётся если отсутствует)
     */
    public TodoCreationData getTodoCreationData(Long chatId) {
        return todoCreationDataMap.computeIfAbsent(chatId, k -> new TodoCreationData());
    }

    /**
     * Очистить данные создания задачи
     */
    public void clearTodoCreationData(Long chatId) {
        todoCreationDataMap.remove(chatId);
    }

    // ========== Методы для работы с временными данными задачи ==========

    /**
     * Установить название задачи
     */
    public void setTempTitle(Long chatId, String title) {
        getTodoCreationData(chatId).setTitle(title);
    }

    /**
     * Установить описание задачи
     */
    public void setTempDescription(Long chatId, String description) {
        getTodoCreationData(chatId).setDescription(description);
    }

    /**
     * Получить название задачи
     */
    public String getTempTitle(Long chatId) {
        return getTodoCreationData(chatId).getTitle();
    }

    /**
     * Получить описание задачи
     */
    public String getTempDescription(Long chatId) {
        return getTodoCreationData(chatId).getDescription();
    }

    /**
     * Очистить все временные данные
     */
    public void clearTempData(Long chatId) {
        clearTodoCreationData(chatId);
    }

    // ========== Методы для работы с фильтрами задач ==========

    /**
     * Установить фильтр статуса задач
     */
    public void setTaskStatusFilter(Long chatId, String filter) {
        taskStatusFilter.put(chatId, filter);
    }

    /**
     * Получить фильтр статуса задач
     */
    public String getTaskStatusFilter(Long chatId) {
        return taskStatusFilter.getOrDefault(chatId, "all");
    }

    /**
     * Установить фильтр дедлайна задач
     */
    public void setTaskDeadlineFilter(Long chatId, String filter) {
        taskDeadlineFilter.put(chatId, filter);
    }

    /**
     * Получить фильтр дедлайна задач
     */
    public String getTaskDeadlineFilter(Long chatId) {
        return taskDeadlineFilter.getOrDefault(chatId, "all");
    }

    /**
     * Сохранить предыдущее состояние перед переходом к новому
     */
    public void savePreviousState(Long chatId) {
        DeaneryState current = getState(chatId);
        previousState.put(chatId, current);
    }

    /**
     * Получить предыдущее состояние
     */
    public DeaneryState getPreviousState(Long chatId) {
        return previousState.get(chatId);
    }

    /**
     * Очистить предыдущее состояние
     */
    public void clearPreviousState(Long chatId) {
        previousState.remove(chatId);
    }
}

