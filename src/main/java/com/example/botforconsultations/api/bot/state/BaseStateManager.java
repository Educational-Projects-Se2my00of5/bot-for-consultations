package com.example.botforconsultations.api.bot.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Базовый менеджер состояний с общей логикой для всех ролей.
 * Использует дженерики для типизации состояний.
 *
 * @param <S> тип enum состояния (UserState, TeacherState, DeaneryState)
 */
public abstract class BaseStateManager<S extends Enum<S>> {

    // Хранилище состояний пользователей
    protected final Map<Long, S> userStates = new HashMap<>();

    // Общее для всех: текущая просматриваемая консультация
    protected final Map<Long, Long> currentConsultationId = new HashMap<>();

    /**
     * Получить дефолтное состояние (должен быть переопределён в наследниках)
     */
    protected abstract S getDefaultState();

    /**
     * Получить текущее состояние пользователя
     */
    public S getState(Long chatId) {
        return userStates.getOrDefault(chatId, getDefaultState());
    }

    /**
     * Установить состояние пользователя
     */
    public void setState(Long chatId, S state) {
        userStates.put(chatId, state);
    }

    /**
     * Сбросить состояние к DEFAULT
     */
    public void resetState(Long chatId) {
        userStates.put(chatId, getDefaultState());
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
     * Очистить ID текущей консультации
     */
    public void clearCurrentConsultation(Long chatId) {
        currentConsultationId.remove(chatId);
    }

    /**
     * Очистить все данные пользователя.
     * Вызывает хук clearSpecificData() для очистки специфичных данных наследника.
     */
    public void clearUserData(Long chatId) {
        userStates.remove(chatId);
        currentConsultationId.remove(chatId);
        clearSpecificData(chatId);
    }

    /**
     * Хук для очистки специфичных данных конкретного менеджера.
     * Переопределяется в наследниках.
     */
    protected abstract void clearSpecificData(Long chatId);
}
