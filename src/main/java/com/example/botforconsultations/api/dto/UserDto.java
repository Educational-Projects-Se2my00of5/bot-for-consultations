package com.example.botforconsultations.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UserDto {
    public record TelegramUserInfo(
            Long id, String firstName, String lastName,
            String phone, String telegramId,
            String role, boolean isActive
    ) {
    }

    public record Login(
            @NotBlank String login, @NotBlank String password
    ) {
    }

    public record Token(
            @NotBlank String token
    ) {
    }

    public record UpdateUser(
            String firstName,
            String lastName
    ) {
    }
}
