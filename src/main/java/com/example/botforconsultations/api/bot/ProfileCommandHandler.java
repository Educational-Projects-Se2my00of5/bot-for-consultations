package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ProfileService;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager.DeaneryState;
import com.example.botforconsultations.api.bot.state.StudentStateManager;
import com.example.botforconsultations.api.bot.state.StudentStateManager.UserState;
import com.example.botforconsultations.api.bot.state.TeacherStateManager;
import com.example.botforconsultations.api.bot.state.TeacherStateManager.TeacherState;
import com.example.botforconsultations.api.bot.utils.StudentKeyboardBuilder;
import com.example.botforconsultations.core.model.ReminderTime;
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
    private final StudentKeyboardBuilder keyboardBuilder;
    private final StudentStateManager studentStateManager;
    private final TeacherStateManager teacherStateManager;
    private final DeaneryStateManager deaneryStateManager;

    /**
     * Обработка команд профиля
     */
    public boolean handleProfileCommand(String text, Long chatId) {
        final TelegramUser user = getCurrentUser(chatId);
        switch (text) {
            case "👤 Профиль" -> showProfile(chatId, user);
            case "✏️ Изменить имя" -> startFirstNameEdit(chatId, user);
            case "✏️ Изменить фамилию" -> startLastNameEdit(chatId, user);
            case "⏰ Время напоминаний" -> startReminderTimeEdit(chatId, user);
            default -> {
                // Проверяем, не выбрано ли время напоминания
                if (text.startsWith("⏱️ ")) {
                    return handleReminderTimeSelection(text, chatId, user);
                }
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
            
            // Показываем время напоминаний для преподавателей
            if (user.isHasConfirmed()) {
                String reminderTime = user.getReminderTime() != null 
                    ? user.getReminderTime().getDisplayName() 
                    : "не установлено";
                message.append(String.format("⏰ Напоминания о задачах: %s\n", reminderTime));
            }
            
            if (!user.isHasConfirmed()) {
                message.append("\n⏳ Ваш аккаунт ожидает подтверждения администратором");
            }
        } else if (role == Role.DEANERY) {
            message.append("Роль: Сотрудник деканата\n");
            if (!user.isHasConfirmed()) {
                message.append("\n⏳ Ваш аккаунт ожидает подтверждения администратором");
            }
        }

        message.append("\n💡 Выберите действие:");

        // Показываем кнопку напоминаний только для подтвержденных преподавателей
        boolean showReminderButton = role == Role.TEACHER && user.isHasConfirmed();

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(message.toString())
                .replyMarkup(keyboardBuilder.buildProfileKeyboard(showReminderButton))
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
        } else if (role == Role.DEANERY) {
            if (user.isHasConfirmed()) {
                deaneryStateManager.setState(chatId, DeaneryState.EDITING_PROFILE_FIRST_NAME);
            } else {
                deaneryStateManager.setState(chatId, DeaneryState.WAITING_APPROVAL_EDITING_FIRST_NAME);
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
        } else if (role == Role.DEANERY) {
            if (user.isHasConfirmed()) {
                deaneryStateManager.setState(chatId, DeaneryState.EDITING_PROFILE_LAST_NAME);
            } else {
                deaneryStateManager.setState(chatId, DeaneryState.WAITING_APPROVAL_EDITING_LAST_NAME);
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

    /**
     * Начать редактирование времени напоминаний (только для преподавателей)
     */
    private void startReminderTimeEdit(Long chatId, TelegramUser user) {
        Role role = user.getRole();

        // Только для преподавателей
        if (role != Role.TEACHER) {
            botMessenger.sendText("⚠️ Настройка времени напоминаний доступна только преподавателям", chatId);
            return;
        }

        // Только для подтвержденных
        if (!user.isHasConfirmed()) {
            botMessenger.sendText("⚠️ Настройка времени напоминаний доступна после подтверждения аккаунта", chatId);
            return;
        }

        teacherStateManager.setState(chatId, TeacherState.EDITING_REMINDER_TIME);

        String currentTime = user.getReminderTime() != null 
            ? user.getReminderTime().getDisplayName() 
            : "не установлено";

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(String.format("⏰ Текущее время напоминаний: %s\n\n" +
                        "Выберите за сколько времени до дедлайна задачи вы хотите получать напоминание:", 
                        currentTime))
                .replyMarkup(keyboardBuilder.buildReminderTimeKeyboard())
                .build());
    }

    /**
     * Обработка выбора времени напоминаний
     */
    private boolean handleReminderTimeSelection(String text, Long chatId, TelegramUser user) {
        ReminderTime selectedTime = parseReminderTime(text);
        
        if (selectedTime == null) {
            return false;
        }

        ProfileService.ProfileUpdateResult result = profileService.updateReminderTime(user, selectedTime);
        botMessenger.sendText(result.message(), chatId);
        
        showProfile(chatId, user);
        return true;
    }

    /**
     * Парсинг времени напоминания из текста кнопки
     */
    private ReminderTime parseReminderTime(String buttonText) {
        return switch (buttonText) {
            case "⏱️ 15 минут" -> ReminderTime.MIN_15;
            case "⏱️ 30 минут" -> ReminderTime.MIN_30;
            case "⏱️ 1 час" -> ReminderTime.HOUR_1;
            case "⏱️ 1 день" -> ReminderTime.DAY_1;
            default -> null;
        };
    }

}
