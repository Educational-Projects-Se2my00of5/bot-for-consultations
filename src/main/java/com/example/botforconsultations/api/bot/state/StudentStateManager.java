package com.example.botforconsultations.api.bot.state;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер состояний студента
 */
@Component
public class StudentStateManager {

    public enum UserState {
        WAITING_FOR_TEACHER_NAME,
        WAITING_FOR_CONSULTATION_MESSAGE,
        VIEWING_CONSULTATION_DETAILS,
        WAITING_FOR_REQUEST_TITLE,     // ожидание названия запроса
        WAITING_FOR_REQUEST_MESSAGE,   // ожидание сообщения для записи на запрос
        VIEWING_REQUEST_DETAILS,       // просмотр деталей запроса
        DEFAULT
    }

    @Getter
    private final Map<Long, UserState> userStates = new HashMap<>();
    @Getter
    private final Map<Long, Long> currentTeacherId = new HashMap<>();
    @Getter
    private final Map<Long, Long> currentConsultationId = new HashMap<>();
    @Getter
    private final Map<Long, Long> currentRequestId = new HashMap<>();  // для запросов консультаций
    @Getter
    private final Map<Long, String> consultationFilter = new HashMap<>();

    /**
     * Получить текущее состояние пользователя
     */
    public UserState getState(Long chatId) {
        return userStates.getOrDefault(chatId, UserState.DEFAULT);
    }

    /**
     * Установить состояние пользователя
     */
    public void setState(Long chatId, UserState state) {
        userStates.put(chatId, state);
    }

    /**
     * Сбросить состояние к DEFAULT
     */
    public void resetState(Long chatId) {
        userStates.put(chatId, UserState.DEFAULT);
    }

    /**
     * Установить текущего преподавателя
     */
    public void setCurrentTeacher(Long chatId, Long teacherId) {
        currentTeacherId.put(chatId, teacherId);
    }

    /**
     * Получить текущего преподавателя
     */
    public Long getCurrentTeacher(Long chatId) {
        return currentTeacherId.get(chatId);
    }

    /**
     * Установить текущую консультацию
     */
    public void setCurrentConsultation(Long chatId, Long consultationId) {
        currentConsultationId.put(chatId, consultationId);
    }

    /**
     * Получить текущую консультацию
     */
    public Long getCurrentConsultation(Long chatId) {
        return currentConsultationId.get(chatId);
    }

    /**
     * Установить текущий запрос консультации
     */
    public void setCurrentRequest(Long chatId, Long requestId) {
        currentRequestId.put(chatId, requestId);
    }

    /**
     * Получить текущий запрос консультации
     */
    public Long getCurrentRequest(Long chatId) {
        return currentRequestId.get(chatId);
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
        currentTeacherId.remove(chatId);
        consultationFilter.remove(chatId);  // Фильтр привязан к преподавателю
    }

    /**
     * Очистить ID текущей консультации
     */
    public void clearCurrentConsultation(Long chatId) {
        currentConsultationId.remove(chatId);
    }

    /**
     * Очистить ID текущего запроса
     */
    public void clearCurrentRequest(Long chatId) {
        currentRequestId.remove(chatId);
    }

    /**
     * Очистить все данные пользователя
     */
    public void clearUserData(Long chatId) {
        userStates.remove(chatId);
        currentTeacherId.remove(chatId);
        currentConsultationId.remove(chatId);
        currentRequestId.remove(chatId);
        consultationFilter.remove(chatId);
    }
}
