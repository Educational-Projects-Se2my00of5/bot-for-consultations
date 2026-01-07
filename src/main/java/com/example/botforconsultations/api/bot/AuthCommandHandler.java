package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.api.bot.utils.DeaneryKeyboardBuilder;
import com.example.botforconsultations.api.bot.utils.KeyboardConstants;
import com.example.botforconsultations.api.bot.utils.TeacherKeyboardBuilder;
import com.example.botforconsultations.core.model.ReminderTime;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandHandler {

    private final BotMessenger botMessenger;
    private final TelegramUserRepository telegramUserRepository;
    private final StudentCommandHandler studentCommands;
    private final TeacherCommandHandler teacherCommands;
    private final DeaneryCommandHandler deaneryCommands;
    private final TeacherKeyboardBuilder teacherKeyboardBuilder;
    private final DeaneryKeyboardBuilder deaneryKeyboardBuilder;

    public void handleStart(Long chatId) {
        Optional<TelegramUser> existingUser = telegramUserRepository.findByTelegramId(chatId);
        if (existingUser.isEmpty()) {
            requestContact(chatId);
        } else if (existingUser.get().getRole() == null) {
            sendRoleSelectionMenu(chatId);
        } else {
            sendMainMenu(chatId, existingUser.get());
        }
    }

    public void handleContact(Contact contact, Long chatId) {
        Optional<TelegramUser> existingUser = telegramUserRepository.findByTelegramId(chatId);

        if (existingUser.isPresent()) {
            botMessenger.sendText("Вы уже зарегистрированы", chatId);
            sendMainMenu(chatId, existingUser.get());
            return;
        }

        // Создаем временную запись пользователя без роли
        TelegramUser user = TelegramUser.builder()
                .telegramId(chatId)
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .phone(contact.getPhoneNumber())
                .build();

        try {
            telegramUserRepository.save(user);
            sendRoleSelectionMenu(chatId);
        } catch (Exception e) {
            if (e.getMessage().contains("uk_telegram_users_name")) {
                botMessenger.sendText("Пользователь с таким именем и фамилией уже существует. " +
                        "Пожалуйста, обратитесь к администратору для уточнения данных.", chatId);
            } else {
                log.error("Error saving user: {}", e.getMessage());
                botMessenger.sendText(
                        "Произошла ошибка при регистрации. Пожалуйста, попробуйте позже или обратитесь к администратору.",
                        chatId);
            }
        }
    }

    public void handleRoleSelection(Long chatId, Role role) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);

        if (userOptional.isPresent()) {
            TelegramUser user = userOptional.get();

            if (user.getRole() != null) {
                botMessenger.sendText("Вы уже зарегистрированы как " +
                        (user.getRole() == Role.TEACHER ? "преподаватель" : "студент"), chatId);
                return;
            }

            user.setRole(role);
            switch (role) {
                case STUDENT -> user.setHasConfirmed(true);
                case TEACHER -> user.setHasConfirmed(false); // Преподаватели должны быть подтверждены администратором
                case DEANERY -> user.setHasConfirmed(false); // Деканат должен быть подтвержден администратором
                case ADMIN -> user.setHasConfirmed(false); // Администраторы должны быть подтверждены
            }
            telegramUserRepository.save(user);

            switch (role) {
                case STUDENT -> {
                    botMessenger.sendText("Вы успешно зарегистрированы как студент!", chatId);
                    studentCommands.sendMainMenu(chatId);
                }
                case TEACHER -> botMessenger.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Вы зарегистрированы как преподаватель.
                                
                                ⏳ Ваш аккаунт ожидает подтверждения администратором.
                                
                                Вы можете отредактировать свой профиль в ожидании подтверждения.""")
                        .replyMarkup(teacherKeyboardBuilder.buildWaitingForApprovalMenu())
                        .build());
                case DEANERY -> botMessenger.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Вы зарегистрированы как сотрудник деканата.
                                
                                ⏳ Ваш аккаунт ожидает подтверждения администратором.
                                
                                Вы можете отредактировать свой профиль в ожидании подтверждения.""")
                        .replyMarkup(deaneryKeyboardBuilder.buildWaitingForApprovalMenu())
                        .build());
                case ADMIN -> botMessenger.sendText("Регистрация администраторов через бот недоступна.", chatId);
            }
        } else {
            botMessenger.sendText(
                    "Пожалуйста, сначала зарегистрируйтесь, отправив команду /start",
                    chatId);
        }
    }

    public void sendMainMenu(Long chatId, TelegramUser telegramUser) {
        switch (telegramUser.getRole()) {
            case STUDENT -> studentCommands.sendMainMenu(chatId);
            case TEACHER -> {
                if (telegramUser.isHasConfirmed()) {
                    teacherCommands.sendMainMenu(chatId);
                } else {
                    teacherCommands.sendWaitingApprovalMenu(chatId);
                }
            }
            case DEANERY -> {
                if (telegramUser.isHasConfirmed()) {
                    deaneryCommands.sendMainMenu(chatId);
                } else {
                    deaneryCommands.sendWaitingApprovalMenu(chatId);
                }
            }
            default -> botMessenger.sendText("Неизвестная ошибка.", chatId);
        }
    }

    public void sendRoleSelectionMenu(Long chatId) {
        sendRoleSelectionMenu(chatId, false);
    }

    /**
     * Меню выбора роли, если allowBack=true, добавляет кнопку НАЗАД.
     */
    public void sendRoleSelectionMenu(Long chatId, boolean allowBack) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Я студент"));
        row1.add(new KeyboardButton("Я преподаватель"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Я сотрудник деканата"));

        List<KeyboardRow> rows = new java.util.ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        if (allowBack) {
            KeyboardRow backRow = new KeyboardRow();
            backRow.add(new KeyboardButton(KeyboardConstants.BACK));
            rows.add(backRow);
        }

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        SendMessage message = SendMessage.builder()
                .text("Выберите вашу роль:")
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        botMessenger.execute(message);
    }


    public void handleRoleChange(Long chatId, Role role) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);
        if (userOptional.isEmpty()) {
            botMessenger.sendText("Пользователь не найден. Отправьте /start.", chatId);
            return;
        }

        TelegramUser user = userOptional.get();

        if (user.getRole() == Role.STUDENT && user.isHasConfirmed()) {
            botMessenger.sendText("Нельзя менять роль подтверждённого студента.", chatId);
            return;
        }

        if (role == user.getRole()) {
            botMessenger.sendText("У вас уже эта роль", chatId);
            return;
        }

        user.setRole(role);
        switch (role) {
            case STUDENT -> user.setHasConfirmed(true);
            case TEACHER -> user.setHasConfirmed(false);
            case DEANERY -> user.setHasConfirmed(false);
            case ADMIN -> user.setHasConfirmed(false);
        }

        telegramUserRepository.save(user);

        switch (role) {
            case STUDENT -> botMessenger.sendText(
                    "Роль изменена — теперь вы студент.", chatId);
            case TEACHER -> botMessenger.sendText(
                    "Роль изменена на преподаватель. Аккаунт ожидает подтверждения.", chatId);
            case DEANERY -> botMessenger.sendText(
                    "Роль изменена на сотрудник деканата. Аккаунт ожидает подтверждения.", chatId);
        }
        sendMainMenu(chatId, user);
    }

    private void requestContact(Long chatId) {
        KeyboardButton contactButton = new KeyboardButton("Поделиться контактом");
        contactButton.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        SendMessage message = SendMessage.builder()
                .text("Для регистрации необходимо поделиться контактом")
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        botMessenger.execute(message);
    }
}
