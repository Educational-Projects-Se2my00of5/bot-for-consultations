package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.StudentConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Сервис для автоматической очистки и закрытия консультаций
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationCleanupService {

    private final ConsultationRepository consultationRepository;
    private final StudentConsultationRepository studentConsultationRepository;

    /**
     * Каждый день в 00:00 проверяет и закрывает прошедшие консультации
     * которые все еще открыты
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void closeExpiredConsultations() {
        log.info("Starting scheduled task: closing expired consultations");

        LocalDate today = LocalDate.now();

        // Находим только открытые консультации с датой в прошлом (оптимизированный запрос)
        List<Consultation> expiredConsultations = consultationRepository
                .findExpiredConsultations(ConsultationStatus.OPEN, today);

        int closedCount = 0;

        for (Consultation consultation : expiredConsultations) {
            consultation.setStatus(ConsultationStatus.CLOSED);
            consultationRepository.save(consultation);
            closedCount++;
            log.info("Closed expired consultation #{} (date: {}, endTime: {})",
                    consultation.getId(),
                    consultation.getDate(),
                    consultation.getEndTime());
        }

        log.info("Scheduled task completed: closed {} expired consultations", closedCount);
    }

    /**
     * Каждый день в 00:05 удаляет консультации, которые завершились более 30 дней назад
     * Сначала удаляет записи студентов, потом сами консультации
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void deleteOldConsultations() {
        log.info("Starting scheduled task: deleting old consultations");

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // Шаг 1: Удаляем все записи студентов для консультаций старше 30 дней
        int deletedRegistrations = studentConsultationRepository.deleteByConsultationDateBefore(thirtyDaysAgo);
        log.info("Deleted {} student registrations for old consultations", deletedRegistrations);

        // Шаг 2: Удаляем сами консультации старше 30 дней
        int deletedConsultations = consultationRepository.deleteByDateBefore(thirtyDaysAgo);
        log.info("Deleted {} old consultations", deletedConsultations);

        log.info("Scheduled task completed: deleted {} consultations and {} registrations",
                deletedConsultations, deletedRegistrations);
    }
}
