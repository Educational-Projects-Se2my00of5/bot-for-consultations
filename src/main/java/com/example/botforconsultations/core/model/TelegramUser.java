package com.example.botforconsultations.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

    // Настройки напоминаний для ToDo (множественный выбор)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_reminder_times", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "reminder_time")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<ReminderTime> reminderTimes = new HashSet<>();

        // Консультации студента - при удалении студента удаляются его записи
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentConsultation> studentConsultations = new ArrayList<>();

    // Консультации преподавателя - при удалении преподавателя удаляются его консультации
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consultation> consultations = new ArrayList<>();

    // Задачи преподавателя - при удалении преподавателя удаляются его задачи
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TodoTask> tasks = new ArrayList<>();

    // Подписки студента - при удалении студента удаляются его подписки
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Subscription> studentSubscriptions = new ArrayList<>();

    // Подписки преподавателя - при удалении преподавателя удаляются подписки на него
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Subscription> teacherSubscriptions = new ArrayList<>();

    // Токен Google Calendar - при удалении пользователя удаляется токен
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private GoogleCalendarToken googleCalendarToken;
}
