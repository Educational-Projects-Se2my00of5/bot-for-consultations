package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.StudentConsultation;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.StudentConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с запросами консультаций (status = REQUEST)
 * Запрос - это консультация без даты/времени, где teacher = студент
 */
@Service
@RequiredArgsConstructor
public class ConsultationRequestService {

    private final ConsultationRepository consultationRepository;
    private final StudentConsultationRepository studentConsultationRepository;

    /**
     * Получить все запросы студента
     */
    public List<Consultation> getStudentRequests(TelegramUser student) {
        return consultationRepository.findByTeacherAndStatusOrderByIdDesc(student, ConsultationStatus.REQUEST);
    }

    /**
     * Получить все запросы всех студентов
     */
    public List<Consultation> getAllRequests() {
        return consultationRepository.findAll().stream()
                .filter(c -> c.getStatus() == ConsultationStatus.REQUEST)
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // сортировка по ID desc
                .toList();
    }

    /**
     * Найти запрос по ID (с проверкой что это действительно запрос)
     */
    public Optional<Consultation> findRequestById(Long id) {
        return consultationRepository.findById(id)
                .filter(c -> c.getStatus() == ConsultationStatus.REQUEST);
    }

    /**
     * Создать запрос консультации
     *
     * @param student студент, создающий запрос
     * @param title   название запроса (и сообщение для записи)
     * @return созданный запрос
     */
    @Transactional
    public Consultation createRequest(TelegramUser student, String title) {
        // Создаём консультацию-запрос
        Consultation request = Consultation.builder()
                .title(title)
                .teacher(student)  // !!! teacher = студент
                .status(ConsultationStatus.REQUEST)
                .date(null)  // !!! дата не заполняется
                .startTime(null)  // !!! время не заполняется
                .endTime(null)
                .capacity(null)
                .autoCloseOnCapacity(false)
                .build();

        request = consultationRepository.save(request);

        // Сразу создаём запись студента на этот запрос
        StudentConsultation studentRecord = StudentConsultation.builder()
                .consultation(request)
                .student(student)
                .message(title)  // message = title
                .build();

        studentConsultationRepository.save(studentRecord);

        return request;
    }

    /**
     * Записать студента на запрос
     *
     * @param student студент
     * @param request запрос консультации
     * @param message сообщение студента (тема/вопрос)
     * @return результат записи
     */
    @Transactional
    public RequestRegistrationResult registerOnRequest(TelegramUser student, Consultation request, String message) {
        // Проверка, что это действительно запрос
        if (request.getStatus() != ConsultationStatus.REQUEST) {
            return failureRegistration("Это не запрос консультации");
        }

        // Проверка: уже записан?
        boolean alreadyRegistered = studentConsultationRepository
                .findByStudentAndConsultation(student, request)
                .isPresent();

        if (alreadyRegistered) {
            return failureRegistration("Вы уже записаны на этот запрос");
        }

        // Создаём запись
        StudentConsultation registration = StudentConsultation.builder()
                .consultation(request)
                .student(student)
                .message(message)
                .build();

        studentConsultationRepository.save(registration);

        return successRegistration();
    }

    /**
     * Отписать студента от запроса
     *
     * @param student студент
     * @param request запрос консультации
     * @return результат отписки
     */
    @Transactional
    public RequestUnregistrationResult unregisterFromRequest(TelegramUser student, Consultation request) {
        // Проверка, что это действительно запрос
        if (request.getStatus() != ConsultationStatus.REQUEST) {
            return failureUnregistration("Это не запрос консультации");
        }

        // Находим запись студента
        Optional<StudentConsultation> registration = studentConsultationRepository
                .findByStudentAndConsultation(student, request);

        if (registration.isEmpty()) {
            return failureUnregistration("Вы не записаны на этот запрос");
        }

        // Удаляем запись студента напрямую
        studentConsultationRepository.delete(registration.get());
        studentConsultationRepository.flush();

        // Проверяем: остались ли ещё записанные студенты?
        long remainingCount = studentConsultationRepository.countByConsultation(request);

        if (remainingCount == 0) {
            // Сначала удаляем ВСЕ оставшиеся записи StudentConsultation
            studentConsultationRepository.deleteByConsultation(request);
            studentConsultationRepository.flush();

            // Теперь можем безопасно удалить сам запрос
            consultationRepository.deleteById(request.getId());
            consultationRepository.flush();
            return successUnregistrationWithDeletion();
        }

        return successUnregistration();
    }

    /**
     * Проверить, записан ли студент на запрос
     */
    public boolean isRegisteredOnRequest(TelegramUser student, Consultation request) {
        return studentConsultationRepository
                .findByStudentAndConsultation(student, request)
                .isPresent();
    }

    /**
     * Результат записи на запрос
     */
    public record RequestRegistrationResult(boolean success, String message) {
    }

    public static RequestRegistrationResult successRegistration() {
        return new RequestRegistrationResult(true, "Вы успешно записались на запрос");
    }

    public static RequestRegistrationResult failureRegistration(String message) {
        return new RequestRegistrationResult(false, message);
    }

    /**
     * Результат отписки от запроса
     */
    public record RequestUnregistrationResult(boolean success, boolean requestDeleted, String message) {
    }

    public static RequestUnregistrationResult successUnregistration() {
        return new RequestUnregistrationResult(true, false, "Вы успешно отписались от запроса");
    }

    public static RequestUnregistrationResult successUnregistrationWithDeletion() {
        return new RequestUnregistrationResult(true, true,
                "Вы были последним записанным студентом. Запрос удалён.");
    }

    public static RequestUnregistrationResult failureUnregistration(String message) {
        return new RequestUnregistrationResult(false, false, message);
    }

    /**
     * Результат создания запроса
     */
    public record CreateRequestResult(
            boolean success,
            String message,
            Consultation request
    ) {
        public static CreateRequestResult success(Consultation request) {
            return new CreateRequestResult(true, "Запрос успешно создан", request);
        }

        public static CreateRequestResult failure(String message) {
            return new CreateRequestResult(false, message, null);
        }
    }
}
