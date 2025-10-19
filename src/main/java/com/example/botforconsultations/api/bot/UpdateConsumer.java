package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final BotMessenger botMessenger;
    private final TelegramUserRepository telegramUserRepository;
    private final StudentCommandHandler studentCommands;
    private final TeacherCommandHandler teacherCommands;
    private final AuthCommandHandler authCommandHandler;


    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            if (message.hasContact()) {
                authCommandHandler.handleContact(message.getContact(), chatId);
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
        } else if (update.hasCallbackQuery()) {
            // Обработка callback-запросов от inline-кнопок
            handleCallbackQuery(update);
        }
    }

    /**
     * Обработка callback-запросов от inline-кнопок
     */
    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        // Формат: "view_consultation:123"
        if (callbackData.startsWith("view_consultation:")) {
            String consultationIdStr = callbackData.substring("view_consultation:".length());
            try {
                Long consultationId = Long.parseLong(consultationIdStr);
                handleViewConsultation(consultationId, chatId);
            } catch (NumberFormatException e) {
                log.error("Invalid consultation ID in callback: {}", callbackData);
                botMessenger.sendText("Ошибка: неверный формат консультации", chatId);
            }
        }
    }

    /**
     * Обработка просмотра консультации из уведомления
     */
    private void handleViewConsultation(Long consultationId, Long chatId) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);

        if (userOptional.isEmpty() || userOptional.get().getRole() != Role.STUDENT) {
            botMessenger.sendText("Эта функция доступна только для студентов", chatId);
            return;
        }

        // Делегируем студенту для показа консультации
        studentCommands.showConsultationFromNotification(consultationId, chatId);
    }

    /**
     * Обработка текстовых сообщений
     */
    private void handleTextMessage(String text, Long chatId) {
        Optional<TelegramUser> userOptional = telegramUserRepository.findByTelegramId(chatId);


        if (text.equals("/start")) {
            authCommandHandler.handleStart(chatId);
            return;
        } else if (text.equals("Я студент")) {
            authCommandHandler.handleRoleSelection(chatId, Role.STUDENT);
            return;
        } else if (text.equals("Я преподаватель")) {
            authCommandHandler.handleRoleSelection(chatId, Role.TEACHER);
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

        // Обрабатываем по роли
        if (user.getRole() == Role.STUDENT) {
            studentCommands.handleStudentCommand(text, chatId);
        } else if (user.getRole() == Role.TEACHER) {
            if (!user.isHasConfirmed()) {
                // Неподтвержденные преподаватели используют специальный обработчик
                teacherCommands.handleUnconfirmedTeacherCommand(text, chatId);
            } else {
                // Подтвержденные преподаватели используют полный функционал
                teacherCommands.handleTeacherCommand(text, chatId);
            }
        }
    }
}
