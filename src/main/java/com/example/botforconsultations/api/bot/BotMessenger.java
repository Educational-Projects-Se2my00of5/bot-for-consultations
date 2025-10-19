package com.example.botforconsultations.api.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotMessenger {

    private final TelegramClient telegramClient;

    // Отправка простого текстового сообщения
    public void sendText(String text, Long chatId) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
        execute(message);
    }

    // Отправка сообщения с inline-клавиатурой
    public void sendTextWithInlineKeyboard(String text, Long chatId, InlineKeyboardMarkup keyboard) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .replyMarkup(keyboard)
                .build();
        execute(message);
    }

    // Унифицированное выполнение отправки сообщения
    public void execute(SendMessage message) {
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
}


