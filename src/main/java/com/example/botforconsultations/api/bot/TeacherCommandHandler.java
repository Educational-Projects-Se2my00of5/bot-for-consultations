package com.example.botforconsultations.api.bot;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCommandHandler {

    private final TelegramUserRepository telegramUserRepository;
    private final ConsultationRepository consultationRepository;
    private final BotMessenger botMessenger;

    private final Map<Long, UserState> userStates = new HashMap<>();

    private enum UserState {
        DEFAULT
    }

    public void handleTeacherCommand(String text, Long chatId) {
        // Проверяем, находится ли пользователь в состоянии ожидания ввода
        UserState currentState = userStates.getOrDefault(chatId, UserState.DEFAULT);

        switch (text) {
            case "Помощь" -> sendHelp(chatId);

            default -> botMessenger.sendText(
                    "Извините, я не понимаю эту команду. Отправьте 'Помощь' для получения списка доступных команд.",
                    chatId
            );
        }
    }


    public void sendHelp(Long chatId) {
        StringBuilder helpText = new StringBuilder();
        helpText.append("Доступные команды для преподавателя:\n\n")
                .append("📅 Мои консультации - управление вашими консультациями\n")
                .append("➕ Создать консультацию - публикация нового времени для консультаций\n")
                .append("📋 Просмотреть запросы - просмотр запросов студентов на консультации\n\n")
                .append("В разделе \"📅 Мои консультации\" вы можете:\n")
                .append("- Просматривать список записавшихся студентов\n")
                .append("- Закрывать запись (можно установить лимит)\n")
                .append("- Отменять консультации\n\n")
                .append("В разделе \"📋 Просмотреть запросы\" можно создавать консультации на основе запросов\n");

        botMessenger.sendText(helpText.toString(), chatId);
    }

    public void sendMainMenu(Long chatId) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow helpRow = new KeyboardRow();

        helpRow.add(new KeyboardButton("Помощь"));
        row1.add(new KeyboardButton("📅 Мои консультации"));
        row1.add(new KeyboardButton("➕ Создать консультацию"));
        row2.add(new KeyboardButton("📋 Просмотреть запросы"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(helpRow);

        botMessenger.execute(
                SendMessage.builder()
                        .text("Добро пожаловать, преподаватель! Выберите действие:")
                        .chatId(chatId)
                        .replyMarkup(
                                ReplyKeyboardMarkup.builder()
                                        .keyboard(keyboard)
                                        .resizeKeyboard(true)
                                        .build()
                        )
                        .build()
        );
    }

}


