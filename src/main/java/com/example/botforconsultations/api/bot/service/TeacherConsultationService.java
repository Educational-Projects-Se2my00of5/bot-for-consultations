package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Сервис для работы с консультациями преподавателя
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherConsultationService {

    private final ConsultationRepository consultationRepository;

    /**
     * Получить все консультации преподавателя
     */
    public List<Consultation> getTeacherConsultations(TelegramUser teacher) {
        return consultationRepository.findByTeacherOrderByStartTimeAsc(teacher);
    }

    /**
     * Создать новую консультацию
     */
    @Transactional
    public Consultation createConsultation(TelegramUser teacher, String title, LocalDate date,
                                           LocalTime startTime, LocalTime endTime,
                                           Integer capacity, boolean autoCloseOnCapacity) {

        // Определяем статус: если автозакрытие включено и capacity = 0, то OPEN
        ConsultationStatus status = ConsultationStatus.OPEN;

        Consultation consultation = Consultation.builder()
                .title(title)
                .teacher(teacher)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .capacity(capacity)
                .autoCloseOnCapacity(autoCloseOnCapacity)
                .status(status)
                .build();

        Consultation saved = consultationRepository.save(consultation);
        log.info("Created consultation #{} by teacher #{}", saved.getId(), teacher.getId());

        return saved;
    }

    /**
     * Закрыть консультацию
     */
    @Transactional
    public void closeConsultation(Consultation consultation) {
        consultation.setStatus(ConsultationStatus.CLOSED);
        consultationRepository.save(consultation);
        log.info("Closed consultation #{}", consultation.getId());
    }

    /**
     * Открыть консультацию
     * Проверяет автозакрытие: если включено и мест нет, то сначала отключает
     */
    @Transactional
    public OpenResult openConsultation(Consultation consultation) {
        long registeredCount = consultation.getRegUsers() != null
                ? consultation.getRegUsers().size()
                : 0;

        // Проверяем: если автозакрытие включено и нет свободных мест
        if (consultation.isAutoCloseOnCapacity() &&
                consultation.getCapacity() != null &&
                registeredCount >= consultation.getCapacity()) {

            // Нужно сначала отключить автозакрытие
            return OpenResult.requiresDisableAutoClose();
        }

        consultation.setStatus(ConsultationStatus.OPEN);
        consultationRepository.save(consultation);
        log.info("Opened consultation #{}", consultation.getId());

        return OpenResult.successful();
    }

    /**
     * Отключить автозакрытие консультации
     */
    @Transactional
    public void disableAutoClose(Consultation consultation) {
        consultation.setAutoCloseOnCapacity(false);
        consultationRepository.save(consultation);
        log.info("Disabled auto-close for consultation #{}", consultation.getId());
    }

    /**
     * Отменить консультацию
     */
    @Transactional
    public void cancelConsultation(Consultation consultation, String reason) {
        consultation.setStatus(ConsultationStatus.CANCELLED);
        consultation.setClosedReason(reason);
        consultationRepository.save(consultation);
        log.info("Cancelled consultation #{}", consultation.getId());
    }

    /**
     * Принять запрос студента и превратить его в консультацию
     *
     * @param request             запрос (consultation со статусом REQUEST)
     * @param teacher             преподаватель, принимающий запрос
     * @param date                дата консультации
     * @param startTime           время начала
     * @param endTime             время окончания
     * @param capacity            вместимость (null = без ограничений)
     * @param autoCloseOnCapacity автозакрытие при достижении лимита
     * @return обновлённая консультация
     */
    @Transactional
    public Consultation acceptRequest(Consultation request, TelegramUser teacher,
                                      LocalDate date, LocalTime startTime, LocalTime endTime,
                                      Integer capacity, boolean autoCloseOnCapacity) {

        // Обновляем запрос, превращая его в консультацию
        request.setTeacher(teacher);  // Меняем автора (студента) на преподавателя
        request.setDate(date);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setCapacity(capacity);
        request.setAutoCloseOnCapacity(autoCloseOnCapacity);

        // Определяем статус
        long interestedCount = request.getRegUsers() != null
                ? request.getRegUsers().size()
                : 0;
        if (autoCloseOnCapacity && capacity != null && interestedCount >= capacity) {
            request.setStatus(ConsultationStatus.CLOSED);
        } else {
            request.setStatus(ConsultationStatus.OPEN);
        }

        Consultation updated = consultationRepository.save(request);
        log.info("Accepted request #{} by teacher #{}, converted to consultation",
                request.getId(), teacher.getId());

        return updated;
    }

    /**
     * Проверить, нужно ли автоматически закрыть консультацию
     * Вызывается после записи студента
     *
     * @param consultationId ID консультации для перезагрузки свежих данных
     */
    @Transactional
    public boolean checkAndAutoClose(Long consultationId) {
        // Перезагружаем консультацию из БД для получения актуальной коллекции regUsers
        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);
        if (consultation == null) {
            return false;
        }

        if (!consultation.isAutoCloseOnCapacity() || consultation.getCapacity() == null) {
            return false;
        }

        long registeredCount = consultation.getRegUsers() != null
                ? consultation.getRegUsers().size()
                : 0;

        if (registeredCount >= consultation.getCapacity() &&
                consultation.getStatus() == ConsultationStatus.OPEN) {

            consultation.setStatus(ConsultationStatus.CLOSED);
            consultationRepository.save(consultation);
            log.info("Auto-closed consultation #{} (capacity reached)", consultation.getId());
            return true;
        }

        return false;
    }

    @Transactional
    public boolean checkAndAutoOpen(Long consultationId, long countBefore) {
        // Перезагружаем консультацию из БД для получения актуальной коллекции regUsers
        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);
        if (consultation == null) {
            return false;
        }

        if (!consultation.isAutoCloseOnCapacity() || consultation.getCapacity() == null) {
            return false;
        }

        long registeredCount = consultation.getRegUsers() != null
                ? consultation.getRegUsers().size()
                : 0;

        if (consultation.getCapacity() == countBefore &&
                registeredCount < countBefore &&
                consultation.getStatus().equals(ConsultationStatus.CLOSED)
        ) {
            consultation.setStatus(ConsultationStatus.OPEN);
            consultationRepository.save(consultation);
            log.info("Auto-open consultation #{} (capacity reached)", consultation.getId());
            return true;
        }

        return false;
    }

    // ========== Result Records ==========

    /**
     * Результат открытия консультации
     */
    public record OpenResult(boolean isSuccess, boolean needsDisableAutoClose, String message) {
        public static OpenResult successful() {
            return new OpenResult(true, false, "Консультация открыта");
        }

        public static OpenResult requiresDisableAutoClose() {
            return new OpenResult(false, true,
                    "Невозможно открыть: все места заняты и включено автозакрытие.\n" +
                            "Сначала отключите автозакрытие или увеличьте вместимость.");
        }
    }
}
