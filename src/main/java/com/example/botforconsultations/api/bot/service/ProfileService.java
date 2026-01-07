package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.ReminderTime;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для управления профилем пользователя
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final TelegramUserRepository telegramUserRepository;

    /**
     * Обновить имя пользователя
     */
    @Transactional
    public ProfileUpdateResult updateFirstName(TelegramUser user, String newFirstName) {
        if (newFirstName == null || newFirstName.trim().isEmpty()) {
            return ProfileUpdateResult.failure("Имя не может быть пустым");
        }

        if (newFirstName.trim().length() > 50) {
            return ProfileUpdateResult.failure("Имя слишком длинное (максимум 50 символов)");
        }

        user.setFirstName(newFirstName.trim());
        telegramUserRepository.save(user);

        return ProfileUpdateResult.success("Имя успешно обновлено");
    }

    /**
     * Обновить фамилию пользователя
     */
    @Transactional
    public ProfileUpdateResult updateLastName(TelegramUser user, String newLastName) {
        if (newLastName == null || newLastName.trim().isEmpty()) {
            return ProfileUpdateResult.failure("Фамилия не может быть пустой");
        }

        if (newLastName.trim().length() > 50) {
            return ProfileUpdateResult.failure("Фамилия слишком длинная (максимум 50 символов)");
        }

        user.setLastName(newLastName.trim());
        telegramUserRepository.save(user);

        return ProfileUpdateResult.success("Фамилия успешно обновлена");
    }

    /**
     * Добавить время напоминаний
     */
    @Transactional
    public ProfileUpdateResult addReminderTime(TelegramUser user, ReminderTime reminderTime) {
        if (reminderTime == null) {
            return ProfileUpdateResult.failure("Некорректное время напоминания");
        }

        if (user.getReminderTimes().contains(reminderTime)) {
            return ProfileUpdateResult.failure("Это время напоминания уже добавлено");
        }

        user.getReminderTimes().add(reminderTime);
        telegramUserRepository.save(user);

        return ProfileUpdateResult.success("✅ Напоминание добавлено: " + reminderTime.getDisplayName() + " до дедлайна");
    }

    /**
     * Удалить время напоминаний
     */
    @Transactional
    public ProfileUpdateResult removeReminderTime(TelegramUser user, ReminderTime reminderTime) {
        if (reminderTime == null) {
            return ProfileUpdateResult.failure("Некорректное время напоминания");
        }

        if (!user.getReminderTimes().contains(reminderTime)) {
            return ProfileUpdateResult.failure("Это время напоминания не установлено");
        }

        user.getReminderTimes().remove(reminderTime);
        telegramUserRepository.save(user);

        return ProfileUpdateResult.success("✅ Напоминание удалено: " + reminderTime.getDisplayName());
    }

    /**
     * Результат обновления профиля
     */
    public record ProfileUpdateResult(boolean success, String message) {
        public static ProfileUpdateResult success(String message) {
            return new ProfileUpdateResult(true, message);
        }

        public static ProfileUpdateResult failure(String message) {
            return new ProfileUpdateResult(false, message);
        }
    }
}
