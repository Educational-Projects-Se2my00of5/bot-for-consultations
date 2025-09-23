package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser extends User {
    private Long telegramId;
    private String username;
    private String fullName;
    private String phone;

    private boolean hasConfirmed;
}
