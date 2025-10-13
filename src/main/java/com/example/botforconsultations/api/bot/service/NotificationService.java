package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.api.bot.BotMessenger;
import com.example.botforconsultations.api.bot.utils.TeacherMessageFormatter;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.Subscription;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для отправки уведомлений студентам
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BotMessenger botMessenger;
    private final SubscriptionRepository subscriptionRepository;
    private final TeacherMessageFormatter messageFormatter;
    private final ConsultationService consultationService;

    /**
     * Уведомить подписчиков о новой консультации
     */
    public void notifySubscribersNewConsultation(Long consultationId) {
        Consultation consultation = consultationService.findById(consultationId);
        List<Subscription> subscriptions = subscriptionRepository.findByTeacher(consultation.getTeacher());
        
        if (subscriptions.isEmpty()) {
            log.debug("No subscribers for teacher #{}", consultation.getTeacher().getId());
            return;
        }

        String message = messageFormatter.formatNewConsultationNotification(consultation);
        
        int sent = 0;
        for (Subscription subscription : subscriptions) {
            Long chatId = subscription.getStudent().getTelegramId();
            try {
                botMessenger.sendText(message, chatId);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send notification to student #{}: {}", 
                        subscription.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Sent {} notifications about new consultation #{}", sent, consultation.getId());
    }

    /**
     * Уведомить записанных студентов об изменении консультации
     */
    public void notifyRegisteredStudentsUpdate(Long consultationId, String changeDescription) {
        Consultation consultation = consultationService.findById(consultationId);
        List<StudentConsultation> registrations = consultation.getRegUsers() != null
                ? List.copyOf(consultation.getRegUsers())
                : List.of();
        
        if (registrations.isEmpty()) {
            log.debug("No registered students for consultation #{}", consultation.getId());
            return;
        }

        String message = messageFormatter.formatConsultationUpdateNotification(consultation, changeDescription);
        
        int sent = 0;
        for (StudentConsultation sc : registrations) {
            Long chatId = sc.getStudent().getTelegramId();
            try {
                botMessenger.sendText(message, chatId);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send update notification to student #{}: {}", 
                        sc.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Sent {} update notifications for consultation #{}", sent, consultation.getId());
    }

    /**
     * Уведомить подписчиков о появлении свободных мест
     * Отправляет только тем, кто подписан на преподавателя, но НЕ записан на консультацию
     */
    public void notifySubscribersAvailableSpots(Long consultationId, Long excludeStudentId) {
        Consultation consultation = consultationService.findById(consultationId);
        // Получаем всех подписчиков преподавателя
        List<Subscription> subscriptions = subscriptionRepository.findByTeacher(consultation.getTeacher());
        
        if (subscriptions.isEmpty()) {
            log.debug("No subscribers for teacher #{}", consultation.getTeacher().getId());
            return;
        }

        // Получаем всех записанных студентов
        List<StudentConsultation> registrations = consultation.getRegUsers() != null
                ? List.copyOf(consultation.getRegUsers())
                : List.of();
        Set<Long> registeredStudentIds = registrations.stream()
                .map(sc -> sc.getStudent().getId())
                .collect(Collectors.toSet());

        // Фильтруем: только подписанные, но не записанные (и не исключённый студент)
        List<TelegramUser> studentsToNotify = subscriptions.stream()
                .map(Subscription::getStudent)
                .filter(student -> !registeredStudentIds.contains(student.getId()))
                .filter(student -> !student.getId().equals(excludeStudentId))
                .toList();

        if (studentsToNotify.isEmpty()) {
            log.debug("No students to notify about available spots for consultation #{}", consultation.getId());
            return;
        }

        long currentCount = registrations.size();
        String message = messageFormatter.formatAvailableSpotsNotification(consultation, currentCount);
        
        int sent = 0;
        for (TelegramUser student : studentsToNotify) {
            Long chatId = student.getTelegramId();
            try {
                botMessenger.sendText(message, chatId);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send available spots notification to student #{}: {}", 
                        student.getId(), e.getMessage());
            }
        }

        log.info("Sent {} available spots notifications for consultation #{}", sent, consultation.getId());
    }

    /**
     * Уведомить записанных студентов об отмене консультации
     */
    public void notifyRegisteredStudentsCancellation(Long consultationId) {
        Consultation consultation = consultationService.findById(consultationId);
        List<StudentConsultation> registrations = consultation.getRegUsers() != null
                ? List.copyOf(consultation.getRegUsers())
                : List.of();
        
        if (registrations.isEmpty()) {
            log.debug("No registered students for consultation #{}", consultation.getId());
            return;
        }

        String message = messageFormatter.formatCancellationNotification(consultation);
        
        int sent = 0;
        for (StudentConsultation sc : registrations) {
            Long chatId = sc.getStudent().getTelegramId();
            try {
                botMessenger.sendText(message, chatId);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send cancellation notification to student #{}: {}", 
                        sc.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Sent {} cancellation notifications for consultation #{}", sent, consultation.getId());
    }

    /**
     * Уведомить записанных студентов о превращении запроса в консультацию
     */
    public void notifyInterestedStudentsRequestAccepted(Long consultationId) {
        Consultation consultation = consultationService.findById(consultationId);
        List<StudentConsultation> registrations = consultation.getRegUsers() != null
                ? List.copyOf(consultation.getRegUsers())
                : List.of();
        
        if (registrations.isEmpty()) {
            log.debug("No interested students for consultation #{}", consultation.getId());
            return;
        }

        // Формируем сообщение как о новой консультации
        String message = messageFormatter.formatNewConsultationNotification(consultation);
        String header = "✅ Ваш запрос принят преподавателем!\n\n";
        message = header + message;
        
        int sent = 0;
        for (StudentConsultation sc : registrations) {
            Long chatId = sc.getStudent().getTelegramId();
            try {
                botMessenger.sendText(message, chatId);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send request accepted notification to student #{}: {}", 
                        sc.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Sent {} request accepted notifications for consultation #{}", sent, consultation.getId());
    }

    /**
     * Уведомить преподавателя о подтверждении аккаунта
     * @param chatId Telegram ID преподавателя
     */
    public void notifyTeacherAccountApproved(Long chatId) {
        String message = """
                ✅ Ваш аккаунт преподавателя подтверждён!
                
                Теперь вы можете:
                📅 Создавать консультации
                📋 Просматривать запросы студентов
                👥 Упрвлять записями на консультаации
                
                Используйте команду /start для начала работы.
                """;
        
        try {
            botMessenger.sendText(message, chatId);
            log.info("Sent account approval notification to teacher with chatId #{}", chatId);
        } catch (Exception e) {
            log.error("Failed to send account approval notification to teacher #{}: {}", 
                    chatId, e.getMessage());
        }
    }
}
