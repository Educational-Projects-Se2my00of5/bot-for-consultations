package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final BotMessenger botMessenger;
    private final TelegramUserRepository telegramUserRepository;
    private final ConsultationRepository consultationRepository;
    private final StudentCommandHandler studentCommands;
    private final TeacherCommandHandler teacherCommands;
    private final AuthHandler authHandler;


    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasContact()) {
                authHandler.handleContact(message.getContact(), chatId);
            } else if (message.hasText()) {
                String text = message.getText();
                handleTextMessage(text, chatId);
            } else {
                botMessenger.sendText(
                        "Извините, я не понимаю эту команду.\n" +
                                "Отправьте 'Помощь' для получения списка доступных команд.\n" +
                                "ИЛИ\n" +
                                "Отправьте '/start' для регистрации.",
                        chatId
                );
            }
        }
    }

    /**
     * Обработка текстовых сообщений
     */
    private void handleTextMessage(String text, Long chatId) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);


        if (text.equals("/start")) {
            authHandler.handleStart(chatId);
            return;
        } else if (text.equals("Я студент")) {
            authHandler.registerUser(chatId, Role.STUDENT);
            return;
        } else if (text.equals("Я преподаватель")) {
            authHandler.registerUser(chatId, Role.TEACHER);
            return;
        }


        // Проверяем, зарегистрирован ли пользователь
        if (userOptional.isEmpty() || userOptional.get().getRole() == null) {
            botMessenger.sendText(
                    "Пожалуйста, сначала зарегистрируйтесь, отправив команду /start",
                    chatId
            );
            return;
        }
        TelegramUser user = userOptional.get();


        if (user.getRole() == Role.STUDENT) {
            studentCommands.handleStudentCommand(text, chatId);
        } else if (user.getRole() == Role.TEACHER) {
            if (!user.isHasConfirmed()) {
                botMessenger.sendText(
                        "Ваша учетная запись ожидает подтверждения администратором",
                        chatId
                );
                return;
            }

            teacherCommands.handleTeacherCommand(text, chatId);
        }
    }

}
