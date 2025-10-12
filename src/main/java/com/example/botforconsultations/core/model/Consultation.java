package com.example.botforconsultations.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDate date;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id")
    private TelegramUser teacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultationStatus status;

    // Вместимость консультации; null или 0 означает без ограничений
    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "auto_close_on_capacity", nullable = false)
    private boolean autoCloseOnCapacity;

    // Причина закрытия или отмены (опционально)
    @Column(name = "closed_reason")
    private String closedReason;

    
    @OneToMany(mappedBy = "consultation", fetch = FetchType.EAGER)
    private Set<StudentConsultation> regUsers;
}