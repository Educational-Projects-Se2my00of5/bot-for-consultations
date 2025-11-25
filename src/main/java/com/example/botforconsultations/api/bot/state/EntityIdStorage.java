package com.example.botforconsultations.api.bot.state;

import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный класс для хранения ID сущностей (композиция).
 * Переиспользуемый компонент для хранения Long ID.
 */
public class EntityIdStorage {
    
    private final Map<Long, Long> storage = new HashMap<>();

    /**
     * Сохранить ID сущности для пользователя
     */
    public void set(Long chatId, Long entityId) {
        storage.put(chatId, entityId);
    }

    /**
     * Получить ID сущности пользователя
     */
    public Long get(Long chatId) {
        return storage.get(chatId);
    }

    /**
     * Очистить ID сущности пользователя
     */
    public void clear(Long chatId) {
        storage.remove(chatId);
    }

    /**
     * Проверить, есть ли сохранённое значение
     */
    public boolean has(Long chatId) {
        return storage.containsKey(chatId);
    }
}
