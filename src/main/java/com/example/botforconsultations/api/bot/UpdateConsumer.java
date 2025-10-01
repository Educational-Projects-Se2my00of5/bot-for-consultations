package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final TelegramUserRepository telegramUserRepository;

    public UpdateConsumer(@Value("${bot.token}") String botToken,
                          TelegramUserRepository telegramUserRepository) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.telegramUserRepository = telegramUserRepository;
    }

    /**
     * Обрабатывает входящие обновления от Telegram.
     * Поддерживает обработку контактов и текстовых сообщений.
     *
     * @param update Объект обновления от Telegram API
     */
    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasContact()) {
                handleContact(message.getContact(), chatId);
            } else if (message.hasText()) {
                String text = message.getText();

                switch (text) {
                    case "/start" -> handleStart(chatId);
                    case "Я студент" -> registerUser(chatId, Role.STUDENT);
                    case "Я преподаватель" -> registerUser(chatId, Role.TEACHER);
                    default -> sendMessage("Извините, я не понимаю эту команду", chatId);
                }
            }
        }
    }

    /**
     * Обрабатывает команду /start.
     * Проверяет существование пользователя и отправляет соответствующее меню.
     *
     * @param chatId ID чата пользователя
     */
    private void handleStart(Long chatId) {
        Optional<TelegramUser> existingUser = telegramUserRepository.findByTelegramId(chatId);
        if (existingUser.isPresent()) {
            sendMainMenu(chatId);
        } else {
            requestContact(chatId);
        }
    }

    /**
     * Обрабатывает полученный контакт пользователя.
     * Создает новую запись пользователя, если он еще не зарегистрирован.
     *
     * @param contact Объект контакта от Telegram
     * @param chatId  ID чата пользователя
     */
    private void handleContact(Contact contact, Long chatId) {
        Optional<TelegramUser> existingUser = telegramUserRepository.findByTelegramId(chatId);
        if (existingUser.isPresent()) {
            sendMessage("Вы уже зарегистрированы", chatId);
            sendMainMenu(chatId);
            return;
        }

        // Создаем временную запись пользователя без роли
        TelegramUser user = TelegramUser.builder()
                .telegramId(chatId)
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .phone(contact.getPhoneNumber())
                .build();

        telegramUserRepository.save(user);
        sendRoleSelectionMenu(chatId);
    }

    /**
     * Регистрирует пользователя с выбранной ролью.
     * Для преподавателей требуется подтверждение администратора.
     *
     * @param chatId ID чата пользователя
     * @param role   Роль пользователя (STUDENT или TEACHER)
     */
    private void registerUser(Long chatId, Role role) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);

        if (userOptional.isPresent()) {
            TelegramUser user = userOptional.get();

            if (user.getRole() != null) {
                sendMessage("Вы уже зарегистрированы как " +
                        (user.getRole() == Role.TEACHER ? "преподаватель" : "студент"), chatId);
                return;
            }

            user.setRole(role);

            switch (role) {
                case STUDENT -> user.setHasConfirmed(true);
                case TEACHER -> user.setHasConfirmed(false); // Преподаватели должны быть подтверждены администратором
            }
            telegramUserRepository.save(user);

            switch (role) {
                case STUDENT -> {
                    sendMessage("Вы успешно зарегистрированы как студент!", chatId);
                    sendMainMenu(chatId);
                }
                case TEACHER -> {
                    sendMessage("Вы зарегистрированы как преподаватель. Ожидайте подтверждения администратором.", chatId);
                }
            }
        } else {
            sendMessage("Пожалуйста, начните с команды /start", chatId);
        }
    }

    /**
     * Отправляет запрос на получение контакта пользователя.
     * Создает кнопку "Поделиться контактом".
     *
     * @param chatId ID чата пользователя
     */
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

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error requesting contact: {}", e.getMessage());
        }
    }

    /**
     * Отправляет меню выбора роли пользователя.
     * Создает кнопки для выбора роли студента или преподавателя.
     *
     * @param chatId ID чата пользователя
     */
    private void sendRoleSelectionMenu(Long chatId) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Я студент"));
        row.add(new KeyboardButton("Я преподаватель"));

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        SendMessage message = SendMessage.builder()
                .text("Выберите вашу роль:")
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending role selection menu: {}", e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для отправки простого текстового сообщения.
     *
     * @param text   Текст сообщения
     * @param chatId ID чата пользователя
     */
    private void sendMessage(String text, Long chatId) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    /**
     * Отправляет главное меню в зависимости от роли пользователя.
     * Для преподавателей проверяет подтверждение аккаунта.
     *
     * @param chatId ID чата пользователя
     */
    private void sendMainMenu(Long chatId) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);
        if (userOptional.isEmpty()) {
            handleStart(chatId);
            return;
        }

        TelegramUser user = userOptional.get();
        if (user.getRole() == null) {
            sendRoleSelectionMenu(chatId);
            return;
        }

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();

        if (user.getRole() == Role.STUDENT) {
            row1.add(new KeyboardButton("Получить консультацию"));
            row1.add(new KeyboardButton("Мои записи"));
        } else if (user.getRole() == Role.TEACHER) {
            if (!user.isHasConfirmed()) {
                sendMessage("Ваша учетная запись ожидает подтверждения администратором", chatId);
                return;
            }
            row1.add(new KeyboardButton("Мои консультации"));
            row1.add(new KeyboardButton("Создать консультацию"));
        }

        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Помощь"));
        keyboard.add(row2);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();

        String welcomeText = user.getRole() == Role.STUDENT ?
                "Добро пожаловать, студент! Выберите действие:" :
                "Добро пожаловать, преподаватель! Выберите действие:";

        SendMessage message = SendMessage.builder()
                .text(welcomeText)
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }
}
