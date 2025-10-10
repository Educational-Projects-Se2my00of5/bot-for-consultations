package com.example.botforconsultations.api.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramClientConfig {

    @Value("${bot.token}")
    private String botToken;

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient(botToken);
    }
}
