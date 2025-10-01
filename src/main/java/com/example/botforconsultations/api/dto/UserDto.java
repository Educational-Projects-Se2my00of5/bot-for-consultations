package com.example.botforconsultations.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UserDto {
    public record TelegramUserInfo(
            Long id, String firstName, String lastName,
            String phone
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
}
