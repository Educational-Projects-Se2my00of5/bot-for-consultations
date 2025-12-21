package com.example.botforconsultations.api.bot.state;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер состояний студента.
 * Наследуется от BaseStateManager с общей логикой.
 */
@Component
public class StudentStateManager extends BaseStateManager<StudentStateManager.UserState> {

    public enum UserState {
        WAITING_FOR_TEACHER_NAME,
        WAITING_FOR_CONSULTATION_MESSAGE,
        VIEWING_CONSULTATION_DETAILS,
        WAITING_FOR_REQUEST_TITLE,     // ожидание названия запроса
        WAITING_FOR_REQUEST_MESSAGE,   // ожидание сообщения для записи на запрос
        VIEWING_REQUEST_DETAILS,       // просмотр деталей запроса
        EDITING_PROFILE_FIRST_NAME,    // редактирование имени
        EDITING_PROFILE_LAST_NAME,     // редактирование фамилии
        WAITING_DELETE_CONFIRMATION,   // подтверждение удаления аккаунта
        DEFAULT
    }

    // Специфичные для студента данные (композиция)
    private final EntityIdStorage teacherIds = new EntityIdStorage();
    private final EntityIdStorage requestIds = new EntityIdStorage();
    private final Map<Long, String> consultationFilter = new HashMap<>();

    @Override
    protected UserState getDefaultState() {
        return UserState.DEFAULT;
    }

    @Override
    protected void clearSpecificData(Long chatId) {
        teacherIds.clear(chatId);
        requestIds.clear(chatId);
        consultationFilter.remove(chatId);
    }

    // ========== Специфичные методы для студента ==========

    /**
     * Установить текущего преподавателя
     */
    public void setCurrentTeacher(Long chatId, Long teacherId) {
        teacherIds.set(chatId, teacherId);
    }

    /**
     * Получить текущего преподавателя
     */
    public Long getCurrentTeacher(Long chatId) {
        return teacherIds.get(chatId);
    }

    /**
     * Установить текущий запрос консультации
     */
    public void setCurrentRequest(Long chatId, Long requestId) {
        requestIds.set(chatId, requestId);
    }

    /**
     * Получить текущий запрос консультации
     */
    public Long getCurrentRequest(Long chatId) {
        return requestIds.get(chatId);
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
        consultationFilter.remove(chatId);  // Фильтр привязан к преподавателю
    }

    /**
     * Очистить ID текущего запроса
     */
    public void clearCurrentRequest(Long chatId) {
        requestIds.clear(chatId);
    }
}
