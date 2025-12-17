package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "telegram_users"
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

    // Настройки напоминаний для ToDo
    @Enumerated(EnumType.STRING)
    private ReminderTime reminderTime;
}
