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
     * @param student студент, создающий запрос
     * @param title название запроса (и сообщение для записи)
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
