package com.example.botforconsultations.api.bot.state;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер состояний для преподавателя.
 * Наследуется от BaseStateManager с общей логикой.
 */
@Slf4j
@Component
public class TeacherStateManager extends BaseStateManager<TeacherStateManager.TeacherState> {

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
        VIEWING_TASK_DETAILS,                 // Просмотр списка задач (+ деталей, если задан currentTaskId)
        ACCEPTING_REQUEST_DATETIME,           // Ввод даты/времени при принятии запроса
        ACCEPTING_REQUEST_CAPACITY,           // Ввод вместимости при принятии запроса
        ACCEPTING_REQUEST_AUTOCLOSE,          // Ввод автозакрытия при принятии запроса
        EDITING_TITLE,                        // Редактирование названия
        EDITING_DATETIME,                     // Редактирование даты и времени
        EDITING_CAPACITY,                     // Редактирование вместимости
        EDITING_AUTOCLOSE,                    // Редактирование автозакрытия
        EDITING_PROFILE_FIRST_NAME,           // Редактирование имени (для активированных)
        EDITING_PROFILE_LAST_NAME,            // Редактирование фамилии (для активированных)
        EDITING_REMINDER_TIME,                // Редактирование времени напоминаний (для активированных)
        WAITING_APPROVAL_EDITING_FIRST_NAME,  // Редактирование имени (для неактивированных)
        WAITING_APPROVAL_EDITING_LAST_NAME    // Редактирование фамилии (для неактивированных)
    }

    /**
     * Data class для временных данных создания консультации (вариант 3).
     * Заменяет 5 отдельных Map на один объект.
     */
    @Getter
    @Setter
    public static class ConsultationCreationData {
        private String title;
        private String date;
        private String startTime;
        private String endTime;
        private Integer capacity;
    }

    // Специфичные для преподавателя данные (композиция)
    private final EntityIdStorage requestIds = new EntityIdStorage();
    private final EntityIdStorage taskIds = new EntityIdStorage();
    private final Map<Long, ConsultationCreationData> creationDataMap = new HashMap<>();
    private final Map<Long, String> taskStatusFilters = new HashMap<>();
    private final Map<Long, String> taskDeadlineFilters = new HashMap<>();

    @Override
    protected TeacherState getDefaultState() {
        return TeacherState.DEFAULT;
    }

    @Override
    protected void clearSpecificData(Long chatId) {
        requestIds.clear(chatId);
        taskIds.clear(chatId);
        creationDataMap.remove(chatId);
        taskStatusFilters.remove(chatId);
        taskDeadlineFilters.remove(chatId);
    }

    // ========== Специфичные методы для преподавателя ==========

    /**
     * Получить ID текущей консультации (alias для совместимости)
     */
    public Long getCurrentConsultationId(Long chatId) {
        return getCurrentConsultation(chatId);
    }

    /**
     * Установить текущий запрос
     */
    public void setCurrentRequest(Long chatId, Long requestId) {
        requestIds.set(chatId, requestId);
    }

    /**
     * Получить ID текущего запроса
     */
    public Long getCurrentRequest(Long chatId) {
        return requestIds.get(chatId);
    }

    /**
     * Очистить текущий запрос
     */
    public void clearCurrentRequest(Long chatId) {
        requestIds.clear(chatId);
        log.debug("Teacher {} current request cleared", chatId);
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
     * Очистить текущую задачу
     */
    public void clearCurrentTask(Long chatId) {
        taskIds.clear(chatId);
        log.debug("Teacher {} current task cleared", chatId);
    }

    // ========== Временные данные для создания консультации ==========

    /**
     * Получить данные создания консультации (автоматически создаётся если отсутствует)
     */
    public ConsultationCreationData getCreationData(Long chatId) {
        return creationDataMap.computeIfAbsent(chatId, k -> new ConsultationCreationData());
    }

    /**
     * Очистить временные данные консультации (после создания)
     */
    public void clearTempConsultationData(Long chatId) {
        creationDataMap.remove(chatId);
        log.debug("Teacher {} temp consultation data cleared", chatId);
    }

    // ========== Методы для обратной совместимости (делегируют к ConsultationCreationData) ==========

    /**
     * Сохранить название консультации
     */
    public void setTempTitle(Long chatId, String title) {
        getCreationData(chatId).setTitle(title);
    }

    /**
     * Получить сохранённое название
     */
    public String getTempTitle(Long chatId) {
        return getCreationData(chatId).getTitle();
    }

    /**
     * Сохранить дату консультации
     */
    public void setTempDate(Long chatId, String date) {
        getCreationData(chatId).setDate(date);
    }

    /**
     * Получить сохранённую дату
     */
    public String getTempDate(Long chatId) {
        return getCreationData(chatId).getDate();
    }

    /**
     * Сохранить время начала
     */
    public void setTempStartTime(Long chatId, String startTime) {
        getCreationData(chatId).setStartTime(startTime);
    }

    /**
     * Получить время начала
     */
    public String getTempStartTime(Long chatId) {
        return getCreationData(chatId).getStartTime();
    }

    /**
     * Сохранить время окончания
     */
    public void setTempEndTime(Long chatId, String endTime) {
        getCreationData(chatId).setEndTime(endTime);
    }

    /**
     * Получить время окончания
     */
    public String getTempEndTime(Long chatId) {
        return getCreationData(chatId).getEndTime();
    }

    /**
     * Сохранить вместимость
     */
    public void setTempCapacity(Long chatId, Integer capacity) {
        getCreationData(chatId).setCapacity(capacity);
    }

    /**
     * Получить вместимость
     */
    public Integer getTempCapacity(Long chatId) {
        return getCreationData(chatId).getCapacity();
    }

    // ========== Фильтры задач ==========

    /**
     * Установить фильтр статуса задач
     */
    public void setTaskStatusFilter(Long chatId, String filter) {
        taskStatusFilters.put(chatId, filter);
    }

    /**
     * Получить фильтр статуса задач
     */
    public String getTaskStatusFilter(Long chatId) {
        return taskStatusFilters.getOrDefault(chatId, "all");
    }

    /**
     * Установить фильтр дедлайна задач
     */
    public void setTaskDeadlineFilter(Long chatId, String filter) {
        taskDeadlineFilters.put(chatId, filter);
    }

    /**
     * Получить фильтр дедлайна задач
     */
    public String getTaskDeadlineFilter(Long chatId) {
        return taskDeadlineFilters.getOrDefault(chatId, "all");
    }
}

