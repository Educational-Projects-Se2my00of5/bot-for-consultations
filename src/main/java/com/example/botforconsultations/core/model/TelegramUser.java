package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;

@Entity
public class TelegramUser extends User {
    private Long telegramId;
    private String username;
    private String phone;
    private boolean hasConfirmed;
}
