package com.example.botforconsultations.api.bot.state;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер состояний для преподавателя
 * Управляет диалоговыми состояниями и временными данными
 */
@Slf4j
@Component
public class TeacherStateManager {

    /**
     * Возможные состояния преподавателя
     */
    public enum TeacherState {
        DEFAULT,                              // Обычное состояние (главное меню)
        WAITING_FOR_CONSULTATION_TITLE,       // Ожидание ввода названия консультации
        WAITING_FOR_CONSULTATION_DATETIME,    // Ожидание ввода даты и времени
        WAITING_FOR_CONSULTATION_CAPACITY,    // Ожидание ввода вместимости
        WAITING_FOR_CONSULTATION_AUTOCLOSE,   // Ожидание ответа об автозакрытии
        VIEWING_CONSULTATION_DETAILS,         // Просмотр списка консультаций (+ деталей, если задан currentConsultationId)
        VIEWING_REQUEST_DETAILS,              // Просмотр списка запросов (+ деталей, если задан currentRequestId)
        ACCEPTING_REQUEST_DATETIME,           // Ввод даты/времени при принятии запроса
        ACCEPTING_REQUEST_CAPACITY,           // Ввод вместимости при принятии запроса
        ACCEPTING_REQUEST_AUTOCLOSE,          // Ввод автозакрытия при принятии запроса
        EDITING_TITLE,                        // Редактирование названия
        EDITING_DATETIME,                     // Редактирование даты и времени
        EDITING_CAPACITY,                     // Редактирование вместимости
        EDITING_AUTOCLOSE                     // Редактирование автозакрытия
    }

    // Состояния пользователей
    private final Map<Long, TeacherState> userStates = new HashMap<>();
    
    // Текущая просматриваемая консультация
    private final Map<Long, Long> currentConsultationId = new HashMap<>();
    
    // Текущий просматриваемый запрос
    private final Map<Long, Long> currentRequestId = new HashMap<>();
    
    // Временные данные для создания консультации
    private final Map<Long, String> tempConsultationTitle = new HashMap<>();
    private final Map<Long, String> tempConsultationDate = new HashMap<>();
    private final Map<Long, String> tempConsultationStartTime = new HashMap<>();
    private final Map<Long, String> tempConsultationEndTime = new HashMap<>();
    private final Map<Long, Integer> tempConsultationCapacity = new HashMap<>();

    /**
     * Получить текущее состояние пользователя
     */
    public TeacherState getState(Long chatId) {
        return userStates.getOrDefault(chatId, TeacherState.DEFAULT);
    }

    /**
     * Установить состояние пользователя
     */
    public void setState(Long chatId, TeacherState state) {
        userStates.put(chatId, state);
        log.debug("Teacher {} state changed to {}", chatId, state);
    }

    /**
     * Сбросить состояние к DEFAULT
     */
    public void resetState(Long chatId) {
        userStates.put(chatId, TeacherState.DEFAULT);
        log.debug("Teacher {} state reset to DEFAULT", chatId);
    }

    /**
     * Установить текущую консультацию
     */
    public void setCurrentConsultation(Long chatId, Long consultationId) {
        currentConsultationId.put(chatId, consultationId);
    }

    /**
     * Получить ID текущей консультации
     */
    public Long getCurrentConsultation(Long chatId) {
        return currentConsultationId.get(chatId);
    }

    /**
     * Получить ID текущей консультации (alias для совместимости)
     */
    public Long getCurrentConsultationId(Long chatId) {
        return currentConsultationId.get(chatId);
    }

    /**
     * Установить текущий запрос
     */
    public void setCurrentRequest(Long chatId, Long requestId) {
        currentRequestId.put(chatId, requestId);
    }

    /**
     * Получить ID текущего запроса
     */
    public Long getCurrentRequest(Long chatId) {
        return currentRequestId.get(chatId);
    }

    /**
     * Очистить текущую консультацию
     */
    public void clearCurrentConsultation(Long chatId) {
        currentConsultationId.remove(chatId);
        log.debug("Teacher {} current consultation cleared", chatId);
    }

    /**
     * Очистить текущий запрос
     */
    public void clearCurrentRequest(Long chatId) {
        currentRequestId.remove(chatId);
        log.debug("Teacher {} current request cleared", chatId);
    }

    // ========== Временные данные для создания консультации ==========

    /**
     * Сохранить название консультации
     */
    public void setTempTitle(Long chatId, String title) {
        tempConsultationTitle.put(chatId, title);
    }

    /**
     * Получить сохранённое название
     */
    public String getTempTitle(Long chatId) {
        return tempConsultationTitle.get(chatId);
    }

    /**
     * Сохранить дату консультации
     */
    public void setTempDate(Long chatId, String date) {
        tempConsultationDate.put(chatId, date);
    }

    /**
     * Получить сохранённую дату
     */
    public String getTempDate(Long chatId) {
        return tempConsultationDate.get(chatId);
    }

    /**
     * Сохранить время начала
     */
    public void setTempStartTime(Long chatId, String startTime) {
        tempConsultationStartTime.put(chatId, startTime);
    }

    /**
     * Получить время начала
     */
    public String getTempStartTime(Long chatId) {
        return tempConsultationStartTime.get(chatId);
    }

    /**
     * Сохранить время окончания
     */
    public void setTempEndTime(Long chatId, String endTime) {
        tempConsultationEndTime.put(chatId, endTime);
    }

    /**
     * Получить время окончания
     */
    public String getTempEndTime(Long chatId) {
        return tempConsultationEndTime.get(chatId);
    }

    /**
     * Сохранить вместимость
     */
    public void setTempCapacity(Long chatId, Integer capacity) {
        tempConsultationCapacity.put(chatId, capacity);
    }

    /**
     * Получить вместимость
     */
    public Integer getTempCapacity(Long chatId) {
        return tempConsultationCapacity.get(chatId);
    }

    /**
     * Очистить все данные пользователя (состояние + временные данные)
     */
    public void clearUserData(Long chatId) {
        userStates.remove(chatId);
        currentConsultationId.remove(chatId);
        currentRequestId.remove(chatId);
        tempConsultationTitle.remove(chatId);
        tempConsultationDate.remove(chatId);
        tempConsultationStartTime.remove(chatId);
        tempConsultationEndTime.remove(chatId);
        tempConsultationCapacity.remove(chatId);
        log.debug("Teacher {} all data cleared", chatId);
    }

    /**
     * Очистить только временные данные консультации (после создания)
     */
    public void clearTempConsultationData(Long chatId) {
        tempConsultationTitle.remove(chatId);
        tempConsultationDate.remove(chatId);
        tempConsultationStartTime.remove(chatId);
        tempConsultationEndTime.remove(chatId);
        tempConsultationCapacity.remove(chatId);
        log.debug("Teacher {} temp consultation data cleared", chatId);
    }
}
