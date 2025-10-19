package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ProfileService;
import com.example.botforconsultations.api.bot.state.StudentStateManager;
import com.example.botforconsultations.api.bot.state.StudentStateManager.UserState;
import com.example.botforconsultations.api.bot.state.TeacherStateManager;
import com.example.botforconsultations.api.bot.state.TeacherStateManager.TeacherState;
import com.example.botforconsultations.api.bot.utils.ProfileKeyboardBuilder;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Обработчик команд профиля (общий для студентов и преподавателей)
 */
@Component
@RequiredArgsConstructor
public class ProfileCommandHandler {

    private final BotMessenger botMessenger;
    private final TelegramUserRepository userRepository;
    private final ProfileService profileService;
    private final ProfileKeyboardBuilder keyboardBuilder;
    private final StudentStateManager studentStateManager;
    private final TeacherStateManager teacherStateManager;

    /**
     * Обработка команд профиля
     */
    public boolean handleProfileCommand(String text, Long chatId) {
        final TelegramUser user = getCurrentUser(chatId);
        switch (text) {
            case "👤 Профиль" -> showProfile(chatId, user);
            case "✏️ Изменить имя" -> startFirstNameEdit(chatId, user);
            case "✏️ Изменить фамилию" -> startLastNameEdit(chatId, user);
            default -> {
                return false;
                // Игнорируем неизвестные команды в контексте профиля
            }
        }
        return true;
    }

    /**
     * Обработка обновления имени
     */
    public void processFirstNameUpdate(String newFirstName, Long chatId, TelegramUser user) {
        ProfileService.ProfileUpdateResult result = profileService.updateFirstName(user, newFirstName);

        botMessenger.sendText(result.message(), chatId);
        showProfile(chatId, user);
    }

    /**
     * Обработка обновления фамилии
     */
    public void processLastNameUpdate(String newLastName, Long chatId, TelegramUser user) {
        ProfileService.ProfileUpdateResult result = profileService.updateLastName(user, newLastName);

        botMessenger.sendText(result.message(), chatId);
        showProfile(chatId, user);
    }

    // ========== Приватные методы ==========

    /**
     * Показать профиль пользователя
     */
    private void showProfile(Long chatId, TelegramUser user) {
        Role role = user.getRole();
        StringBuilder message = new StringBuilder();

        message.append("👤 Ваш профиль\n\n");
        message.append(String.format("Имя: %s\n", user.getFirstName()));
        message.append(String.format("Фамилия: %s\n",
                user.getLastName() != null ? user.getLastName() : "(не указана)"));
        message.append(String.format("Телефон: %s\n", user.getPhone()));

        if (role == Role.STUDENT) {
            message.append("Роль: Студент\n");
        } else if (role == Role.TEACHER) {
            message.append("Роль: Преподаватель\n");
            if (!user.isHasConfirmed()) {
                message.append("\n⏳ Ваш аккаунт ожидает подтверждения администратором");
            }
        }

        message.append("\n💡 Выберите действие:");

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(message.toString())
                .replyMarkup(keyboardBuilder.buildProfileKeyboard())
                .build());
    }

    /**
     * Начать редактирование имени
     */
    private void startFirstNameEdit(Long chatId, TelegramUser user) {
        Role role = user.getRole();

        if (role == Role.STUDENT) {
            studentStateManager.setState(chatId, UserState.EDITING_PROFILE_FIRST_NAME);
        } else if (role == Role.TEACHER) {
            if (user.isHasConfirmed()) {
                teacherStateManager.setState(chatId, TeacherState.EDITING_PROFILE_FIRST_NAME);
            } else {
                teacherStateManager.setState(chatId, TeacherState.WAITING_APPROVAL_EDITING_FIRST_NAME);
            }
        }

        botMessenger.sendText(
                String.format("Текущее имя: %s\n\nВведите новое имя:", user.getFirstName()),
                chatId
        );
    }

    /**
     * Начать редактирование фамилии
     */
    private void startLastNameEdit(Long chatId, TelegramUser user) {
        Role role = user.getRole();

        if (role == Role.STUDENT) {
            studentStateManager.setState(chatId, UserState.EDITING_PROFILE_LAST_NAME);
        } else if (role == Role.TEACHER) {
            if (user.isHasConfirmed()) {
                teacherStateManager.setState(chatId, TeacherState.EDITING_PROFILE_LAST_NAME);
            } else {
                teacherStateManager.setState(chatId, TeacherState.WAITING_APPROVAL_EDITING_LAST_NAME);
            }
        }

        String currentLastName = user.getLastName() != null ? user.getLastName() : "(не указана)";
        botMessenger.sendText(
                String.format("Текущая фамилия: %s\n\nВведите новую фамилию:", currentLastName),
                chatId
        );
    }

    /**
     * Получить текущего пользователя
     */
    private TelegramUser getCurrentUser(Long chatId) {
        return userRepository.findByTelegramId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }

}
