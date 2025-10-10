package com.example.botforconsultations.core.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscriptions_student_teacher",
                columnNames = {"student_id", "teacher_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private TelegramUser student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TelegramUser teacher;
}


