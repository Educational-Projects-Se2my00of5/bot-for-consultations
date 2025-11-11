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
        CREATING_TODO_TITLE,               // Ввод названия задачи
        CREATING_TODO_DESCRIPTION,         // Ввод описания задачи
        CREATING_TODO_DEADLINE,            // Ввод дедлайна задачи
        
        // Состояния редактирования профиля для неподтвержденных пользователей
        WAITING_APPROVAL_EDITING_FIRST_NAME,  // Ожидание ввода нового имени (неподтвержденный)
        WAITING_APPROVAL_EDITING_LAST_NAME,   // Ожидание ввода новой фамилии (неподтвержденный)
        
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
    private final Map<Long, String> consultationFilter = new HashMap<>();
    private final Map<Long, TodoCreationData> todoCreationDataMap = new HashMap<>();

    @Override
    protected DeaneryState getDefaultState() {
        return DeaneryState.DEFAULT;
    }

    @Override
    protected void clearSpecificData(Long chatId) {
        teacherIds.clear(chatId);
        consultationIds.clear(chatId);
        consultationFilter.remove(chatId);
        todoCreationDataMap.remove(chatId);
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

    /**
     * Полная очистка всех данных пользователя (при выходе в главное меню)
     */
    public void clearUserData(Long chatId) {
        resetState(chatId);
        clearCurrentTeacher(chatId);
        clearTempData(chatId);
    }
}

