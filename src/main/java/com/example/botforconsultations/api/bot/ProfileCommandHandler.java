package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.service.ProfileService;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager;
import com.example.botforconsultations.api.bot.state.DeaneryStateManager.DeaneryState;
import com.example.botforconsultations.api.bot.state.StudentStateManager;
import com.example.botforconsultations.api.bot.state.StudentStateManager.UserState;
import com.example.botforconsultations.api.bot.state.TeacherStateManager;
import com.example.botforconsultations.api.bot.state.TeacherStateManager.TeacherState;
import com.example.botforconsultations.api.bot.utils.KeyboardConstants;
import com.example.botforconsultations.api.bot.utils.StudentKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.TeacherKeyboardBuilder;
import com.example.botforconsultations.core.model.ReminderTime;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Обработчик команд профиля (общий для студентов и преподавателей)
 */
@Component
@RequiredArgsConstructor
public class ProfileCommandHandler {

    private final BotMessenger botMessenger;
    private final TelegramUserRepository telegramUserRepository;
    private final ProfileService profileService;
    private final StudentKeyboardBuilder keyboardBuilder;
    private final TeacherKeyboardBuilder teacherKeyboardBuilder;
    private final StudentStateManager studentStateManager;
    private final TeacherStateManager teacherStateManager;
    private final DeaneryStateManager deaneryStateManager;
    private final GoogleOAuthService googleOAuthService;

    /**
     * Обработка команд профиля
     */
    public boolean handleProfileCommand(String text, Long chatId) {
        final TelegramUser user = getCurrentUser(chatId);

        if (handleWaitingDeleteConfirmations(text, chatId, user)) {
            return true;
        }
        switch (text) {
            case "👤 Профиль" -> showProfile(chatId, user);
            case "✏️ Изменить имя" -> startFirstNameEdit(chatId, user);
            case "✏️ Изменить фамилию" -> startLastNameEdit(chatId, user);
            case "⏰ Время напоминаний" -> startReminderTimeEdit(chatId, user);
            case KeyboardConstants.ADD_REMINDER_TIME -> startAddReminderTime(chatId, user);
            case KeyboardConstants.REMOVE_REMINDER_TIME -> startRemoveReminderTime(chatId, user);
            case "🔗 Подключить Google Calendar" -> handleConnectGoogleCalendar(chatId, user);
            case "🔓 Отключить Google Calendar" -> handleDisconnectGoogleCalendar(chatId, user);
            case KeyboardConstants.DELETE_ACCOUNT -> startDeleteConfirmation(chatId, user);
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

    /**
     * Обработка добавления времени напоминания (вызывается из TeacherCommandHandler)
     */
    public void processAddReminderTime(String text, Long chatId) {
        TelegramUser user = getCurrentUser(chatId);
        ReminderTime selectedTime = parseReminderTime(text);
        
        if (selectedTime == null) {
            botMessenger.sendText("⚠️ Неизвестное время напоминания", chatId);
            return;
        }

        ProfileService.ProfileUpdateResult result = profileService.addReminderTime(user, selectedTime);
        botMessenger.sendText(result.message(), chatId);
    }

    /**
     * Обработка удаления времени напоминания (вызывается из TeacherCommandHandler)
     */
    public void processRemoveReminderTime(String text, Long chatId) {
        TelegramUser user = getCurrentUser(chatId);
        ReminderTime selectedTime = parseReminderTime(text);
        
        if (selectedTime == null) {
            botMessenger.sendText("⚠️ Неизвестное время напоминания", chatId);
            return;
        }

        ProfileService.ProfileUpdateResult result = profileService.removeReminderTime(user, selectedTime);
        botMessenger.sendText(result.message(), chatId);
    }

    /**
     * Начать редактирование времени напоминаний (публичный для TeacherCommandHandler)
     */
    public void startReminderTimeEdit(Long chatId, TelegramUser user) {
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

        String currentTimesStr = formatReminderTimes(user.getReminderTimes());

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                                ⏰ Текущие напоминания: %s
                                
                                Выберите действие:""",
                        currentTimesStr))
                .replyMarkup(teacherKeyboardBuilder.buildReminderTimeMenuKeyboard())
                .build());
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
                String reminderTimesStr = formatReminderTimes(user.getReminderTimes());
                message.append(String.format("⏰ Напоминания о задачах: %s\n", reminderTimesStr));

                // Показываем статус подключения Google Calendar
                boolean isCalendarConnected = googleOAuthService.isConnected(user);
                if (isCalendarConnected) {
                    message.append("📅 Google Calendar: подключен\n");
                } else {
                    message.append("📅 Google Calendar: не подключен\n");
                }
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

        // Параметры для клавиатуры
        boolean showReminderButton = role == Role.TEACHER && user.isHasConfirmed();
        boolean isCalendarConnected = role == Role.TEACHER && user.isHasConfirmed() && googleOAuthService.isConnected(user);
        boolean showConnectCalendar = role == Role.TEACHER && user.isHasConfirmed() && !isCalendarConnected;
        boolean showDisconnectCalendar = role == Role.TEACHER && user.isHasConfirmed() && isCalendarConnected;

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text(message.toString())
                .replyMarkup(keyboardBuilder.buildProfileKeyboard(
                        showReminderButton,
                        showConnectCalendar,
                        showDisconnectCalendar))
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
        return telegramUserRepository.findByTelegramId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }

    /**
     * Начать добавление времени напоминания
     */
    private void startAddReminderTime(Long chatId, TelegramUser user) {
        Set<ReminderTime> existingTimes = user.getReminderTimes();
        
        if (existingTimes.size() >= ReminderTime.values().length) {
            botMessenger.sendText("⚠️ Все доступные времена напоминаний уже добавлены", chatId);
            return;
        }

        teacherStateManager.setState(chatId, TeacherState.ADDING_REMINDER_TIME);

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("Выберите время напоминания для добавления:")
                .replyMarkup(teacherKeyboardBuilder.buildAddReminderTimeKeyboard(existingTimes))
                .build());
    }

    /**
     * Начать удаление времени напоминания
     */
    private void startRemoveReminderTime(Long chatId, TelegramUser user) {
        Set<ReminderTime> existingTimes = user.getReminderTimes();
        
        if (existingTimes.isEmpty()) {
            botMessenger.sendText("⚠️ У вас нет установленных напоминаний для удаления", chatId);
            return;
        }

        teacherStateManager.setState(chatId, TeacherState.REMOVING_REMINDER_TIME);

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("Выберите время напоминания для удаления:")
                .replyMarkup(teacherKeyboardBuilder.buildRemoveReminderTimeKeyboard(existingTimes))
                .build());
    }

    private void startDeleteConfirmation(Long chatId, TelegramUser user) {
        Role role = user.getRole();
        if (role == Role.STUDENT) {
            studentStateManager.setState(chatId, UserState.WAITING_DELETE_CONFIRMATION);
        } else if (role == Role.TEACHER) {
            teacherStateManager.setState(chatId, TeacherState.WAITING_DELETE_CONFIRMATION);
        } else if (role == Role.DEANERY) {
            deaneryStateManager.setState(chatId, DeaneryState.WAITING_DELETE_CONFIRMATION);
        }

        botMessenger.execute(SendMessage.builder()
                .chatId(chatId)
                .text("Вы уверены, что хотите удалить аккаунт? Это действие необратимо.")
                .replyMarkup(keyboardBuilder.buildConfirmDeleteKeyboard())
                .build());
    }

    private void performAccountDeletion(TelegramUser user, Long chatId) {
        // Удаляем аккаунт (каскадное удаление связанных данных)
        telegramUserRepository.delete(user);
        // Очищаем состояния
        studentStateManager.clearUserData(chatId);
        teacherStateManager.clearUserData(chatId);
        deaneryStateManager.clearUserData(chatId);

        botMessenger.sendText("Ваш аккаунт удалён. Чтобы зарегистрироваться снова, отправьте /start.", chatId);
    }

    private boolean handleWaitingDeleteConfirmations(String text, Long chatId, TelegramUser user) {
        if (studentStateManager.getState(chatId) == UserState.WAITING_DELETE_CONFIRMATION) {
            if (text.equals(KeyboardConstants.CONFIRM_DELETE)) {
                performAccountDeletion(user, chatId);
                return true;
            } else if (text.equals(KeyboardConstants.CANCEL)) {
                studentStateManager.resetState(chatId);
                showProfile(chatId, user);
                return true;
            }
        }

        if (teacherStateManager.getState(chatId) == TeacherState.WAITING_DELETE_CONFIRMATION) {
            if (text.equals(KeyboardConstants.CONFIRM_DELETE)) {
                performAccountDeletion(user, chatId);
                return true;
            } else if (text.equals(KeyboardConstants.CANCEL)) {
                teacherStateManager.resetState(chatId);
                showProfile(chatId, user);
                return true;
            }
        }

        if (deaneryStateManager.getState(chatId) == DeaneryState.WAITING_DELETE_CONFIRMATION) {
            if (text.equals(KeyboardConstants.CONFIRM_DELETE)) {
                performAccountDeletion(user, chatId);
                return true;
            } else if (text.equals(KeyboardConstants.CANCEL)) {
                deaneryStateManager.resetState(chatId);
                showProfile(chatId, user);
                return true;
            }
        }

        return false;
    }

    /**
     * Обработка выбора времени напоминаний (добавление или удаление)
     */
    private boolean handleReminderTimeSelection(String text, Long chatId, TelegramUser user) {
        ReminderTime selectedTime = parseReminderTime(text);

        if (selectedTime == null) {
            return false;
        }

        TeacherState state = teacherStateManager.getState(chatId);
        
        if (state == TeacherState.ADDING_REMINDER_TIME) {
            ProfileService.ProfileUpdateResult result = profileService.addReminderTime(user, selectedTime);
            botMessenger.sendText(result.message(), chatId);
            teacherStateManager.resetState(chatId);
            showProfile(chatId, user);
            return true;
        } else if (state == TeacherState.REMOVING_REMINDER_TIME) {
            ProfileService.ProfileUpdateResult result = profileService.removeReminderTime(user, selectedTime);
            botMessenger.sendText(result.message(), chatId);
            teacherStateManager.resetState(chatId);
            showProfile(chatId, user);
            return true;
        }

        return false;
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

    /**
     * Форматирование списка времён напоминаний для отображения
     */
    private String formatReminderTimes(Set<ReminderTime> times) {
        if (times == null || times.isEmpty()) {
            return "не установлено";
        }
        return times.stream()
                .map(ReminderTime::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Обработка подключения Google Calendar
     */
    private void handleConnectGoogleCalendar(Long chatId, TelegramUser user) {
        try {
            // Генерируем URL для авторизации
            String authUrl = googleOAuthService.getAuthorizationUrl(user.getId());

            String message = String.format("""
                    🔗 Подключение Google Calendar
                    
                    Для подключения вашего календаря Google выполните следующие шаги:
                    
                    1️⃣ Перейдите по ссылке ниже
                    2️⃣ Войдите в свой аккаунт Google
                    3️⃣ Разрешите доступ к календарю
                    4️⃣ После авторизации вы получите уведомление в боте
                    
                    🔗 Ссылка для авторизации:
                    %s
                    
                    ℹ️ После подключения все ваши активные задачи будут добавлены в календарь, а новые задачи будут автоматически синхронизироваться.
                    """, authUrl);

            botMessenger.sendText(message, chatId);
        } catch (Exception e) {
            botMessenger.sendText("❌ Ошибка при создании ссылки для авторизации. Попробуйте позже.", chatId);
        }
    }

    /**
     * Обработка отключения Google Calendar
     */
    private void handleDisconnectGoogleCalendar(Long chatId, TelegramUser user) {
        try {
            googleOAuthService.disconnect(user);

            String message = """
                    ✅ Google Calendar отключен
                    
                    Синхронизация с Google Calendar отключена.
                    Существующие события в календаре сохранятся, но новые задачи не будут добавляться.
                    
                    Вы можете подключить календарь снова в любой момент через профиль.
                    """;

            botMessenger.sendText(message, chatId);
            showProfile(chatId, user);
        } catch (Exception e) {
            botMessenger.sendText("❌ Ошибка при отключении Google Calendar.", chatId);
        }
    }

}
