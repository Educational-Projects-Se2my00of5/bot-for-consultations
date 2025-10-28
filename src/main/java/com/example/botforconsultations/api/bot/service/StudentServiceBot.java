package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.Subscription;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.StudentConsultationRepository;
import com.example.botforconsultations.core.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления подписками студентов и записями на консультации
 */
@Service
@RequiredArgsConstructor
public class StudentServiceBot {

    private final SubscriptionRepository subscriptionRepository;
    private final StudentConsultationRepository studentConsultationRepository;

    // ========== Подписки ==========

    /**
     * Подписать студента на обновления преподавателя
     */
    @Transactional
    public SubscriptionResult subscribe(TelegramUser student, TelegramUser teacher) {
        if (subscriptionRepository.existsByStudentAndTeacher(student, teacher)) {
            return SubscriptionResult.alreadySubscribed();
        }

        Subscription subscription = Subscription.builder()
                .student(student)
                .teacher(teacher)
                .build();
        subscriptionRepository.save(subscription);

        return SubscriptionResult.success(true);
    }

    /**
     * Отписать студента от обновлений преподавателя
     */
    @Transactional
    public SubscriptionResult unsubscribe(TelegramUser student, TelegramUser teacher) {
        if (!subscriptionRepository.existsByStudentAndTeacher(student, teacher)) {
            return SubscriptionResult.notSubscribed();
        }

        subscriptionRepository.deleteByStudentAndTeacher(student, teacher);
        return SubscriptionResult.success(false);
    }

    /**
     * Проверить, подписан ли студент на преподавателя
     */
    public boolean isSubscribed(TelegramUser student, TelegramUser teacher) {
        return subscriptionRepository.existsByStudentAndTeacher(student, teacher);
    }

    /**
     * Получить все подписки студента
     */
    public List<Subscription> getStudentSubscriptions(TelegramUser student) {
        return subscriptionRepository.findByStudent(student);
    }

    // ========== Записи на консультации ==========

    /**
     * Записать студента на консультацию
     */
    @Transactional
    public RegistrationResult register(TelegramUser student, Consultation consultation, String message) {
        if (studentConsultationRepository.existsByStudentAndConsultation(student, consultation)) {
            return RegistrationResult.alreadyRegistered();
        }

        StudentConsultation studentConsultation = StudentConsultation.builder()
                .student(student)
                .consultation(consultation)
                .message(message)
                .build();

        studentConsultationRepository.save(studentConsultation);
        return RegistrationResult.success(true);
    }

    /**
     * Отменить запись студента на консультацию
     */
    @Transactional
    public RegistrationResult cancelRegistration(TelegramUser student, Consultation consultation) {
        Optional<StudentConsultation> registration =
                studentConsultationRepository.findByStudentAndConsultation(student, consultation);

        if (registration.isEmpty()) {
            return RegistrationResult.notRegistered();
        }

        studentConsultationRepository.delete(registration.get());
        return RegistrationResult.success(false);
    }

    /**
     * Проверить, записан ли студент на консультацию
     */
    public boolean isRegistered(TelegramUser student, Consultation consultation) {
        return studentConsultationRepository.existsByStudentAndConsultation(student, consultation);
    }

    /**
     * Получить регистрацию студента на консультацию
     */
    public Optional<StudentConsultation> getStudentRegistration(TelegramUser student, Consultation consultation) {
        return studentConsultationRepository.findByStudentAndConsultation(student, consultation);
    }

    /**
     * Получить все записи студента (только консультации, без запросов)
     */
    public List<StudentConsultation> getStudentRegistrations(TelegramUser student) {
        return studentConsultationRepository.findByStudentExcludingRequests(student);
    }

    /**
     * Получить количество записанных студентов на консультацию
     */
    public long getRegisteredCount(Consultation consultation) {
        return consultation.getRegUsers() != null
                ? consultation.getRegUsers().size()
                : 0;
    }

    // ========== Результаты операций ==========

    public record SubscriptionResult(boolean success, String message, boolean isSubscribed) {
        public static SubscriptionResult success(boolean isSubscribed) {
            String msg = isSubscribed ? "Успешно подписались" : "Успешно отписались";
            return new SubscriptionResult(true, msg, isSubscribed);
        }

        public static SubscriptionResult alreadySubscribed() {
            return new SubscriptionResult(false, "Вы уже подписаны на обновления этого преподавателя", true);
        }

        public static SubscriptionResult notSubscribed() {
            return new SubscriptionResult(false, "Вы не подписаны на этого преподавателя", false);
        }
    }

    public record RegistrationResult(boolean success, String message, boolean isRegistered) {
        public static RegistrationResult success(boolean isRegistered) {
            String msg = isRegistered ? "Успешно записались" : "Запись отменена";
            return new RegistrationResult(true, msg, isRegistered);
        }

        public static RegistrationResult alreadyRegistered() {
            return new RegistrationResult(false, "Вы уже записаны на эту консультацию", true);
        }

        public static RegistrationResult notRegistered() {
            return new RegistrationResult(false, "Вы не записаны на эту консультацию", false);
        }
    }
}
