package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Сервис для работы с консультациями
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

    /**
     * Получить консультации преподавателя с фильтром
     */
    public List<Consultation> getTeacherConsultations(TelegramUser teacher, String filter) {
        List<Consultation> allConsultations = consultationRepository.findByTeacherOrderByStartTimeAsc(teacher);
        List<Consultation> requests = consultationRepository.findByTeacherAndStatusOrderByIdDesc(teacher, ConsultationStatus.REQUEST);

        // Исключаем запросы из всех консультаций
        List<Consultation> filteredConsultations = allConsultations.stream()
                .filter(consultation -> requests.stream().noneMatch(request -> request.getId().equals(consultation.getId())))
                .toList();

        return applyFilter(filteredConsultations, filter);
    }

    /**
     * Применяет фильтр к списку консультаций
     */
    public List<Consultation> applyFilter(List<Consultation> consultations, String filter) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return switch (filter) {
            case "future" -> consultations.stream()
                    .filter(c -> c.getDate().isAfter(today) ||
                            (c.getDate().isEqual(today) && c.getStartTime().isAfter(now)))
                    .toList();
            case "past" -> consultations.stream()
                    .filter(c -> c.getDate().isBefore(today) ||
                            (c.getDate().isEqual(today) && c.getStartTime().isBefore(now)))
                    .toList();
            default -> consultations; // "all"
        };
    }

    /**
     * Найти консультацию по ID
     */
    public Consultation findById(Long id) {
        return consultationRepository.findById(id).orElse(null);
    }

    /**
     * Проверить, доступна ли консультация для записи
     */
    public ValidationResult validateForRegistration(Consultation consultation, long currentRegisteredCount) {
        if (consultation == null) {
            return ValidationResult.error("Консультация не найдена");
        }

        // Проверяем статус
        if (consultation.getStatus() != null) {
            switch (consultation.getStatus()) {
                case CANCELLED -> {
                    return ValidationResult.error("Эта консультация отменена");
                }
                case CLOSED -> {
                    return ValidationResult.error("Запись на эту консультацию закрыта");
                }
                case OPEN, REQUEST -> {
                    // Консультация доступна для записи
                }
            }
        }

        // Проверяем вместимость
        if (consultation.getCapacity() != null && consultation.getCapacity() > 0) {
            if (currentRegisteredCount >= consultation.getCapacity()) {
                return ValidationResult.error(
                        String.format("К сожалению, все места на эту консультацию уже заняты.\nЗаписано: %d/%d",
                                currentRegisteredCount, consultation.getCapacity())
                );
            }
        }

        return ValidationResult.success();
    }

    /**
     * Результат валидации
     */
    public record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
