package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(
    name = "telegram_users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_telegram_users_name",
            columnNames = {"firstName", "lastName"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TelegramUser extends User {
    private Long telegramId;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean hasConfirmed;
}
