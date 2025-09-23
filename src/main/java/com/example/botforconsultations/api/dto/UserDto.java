package com.example.botforconsultations.api.dto;

import com.example.botforconsultations.core.model.TelegramUser;

public class UserDto {
    public record TelegramUserInfo(
            Long id, String username,
            String fullName, String phone
    ) {
    }
}
