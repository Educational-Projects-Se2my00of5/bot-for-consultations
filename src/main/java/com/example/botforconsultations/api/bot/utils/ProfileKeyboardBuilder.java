package com.example.botforconsultations.api.bot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для построения клавиатуры профиля
 */
@Component
public class ProfileKeyboardBuilder {

    /**
     * Клавиатура меню профиля
     */
    public ReplyKeyboardMarkup buildProfileKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("✏️ Изменить имя"));
        row1.add(new KeyboardButton("✏️ Изменить фамилию"));
        keyboard.add(row1);

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("◀️ Назад"));
        keyboard.add(backRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }
}
