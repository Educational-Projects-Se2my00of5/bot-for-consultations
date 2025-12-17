package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.dto.UserGenerateDto;
import com.example.botforconsultations.core.exception.BadRequestException;
import com.example.botforconsultations.core.model.Consultation;
import com.example.botforconsultations.core.model.ConsultationStatus;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.User;
import com.example.botforconsultations.core.repository.ConsultationRepository;
import com.example.botforconsultations.core.repository.StudentConsultationRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final UserRepository userRepository;
    private final StudentConsultationRepository studentConsultationRepository;
    private final ConsultationRepository consultationRepository;
    private final TelegramUserRepository telegramUserRepository;

    @PostMapping("generate/teacher")
    @Operation(summary = "Генерация тестового преподавателя")
    @ResponseStatus(HttpStatus.OK)
    public void generateTeacher(
            @RequestBody UserGenerateDto dto
    ) {
        TelegramUser telegramUser = TelegramUser
                .builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .hasConfirmed(false)
                .build();
        telegramUser.setRole(Role.TEACHER);
        telegramUserRepository.save(telegramUser);
    }

    @PostMapping("generate/student")
    @Operation(summary = "Генерация тестового студента")
    @ResponseStatus(HttpStatus.OK)
    public void generateStudent(
            @RequestBody UserGenerateDto dto
    ) {
        TelegramUser telegramUser = TelegramUser
                .builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .hasConfirmed(true)
                .build();
        telegramUser.setRole(Role.STUDENT);
        telegramUserRepository.save(telegramUser);
    }

    @GetMapping("shedule/close_ended")
    @Operation(summary = "Проверка закрытия консультаций которые прошли")
    @ResponseStatus(HttpStatus.OK)
    public void testCloseEnded() {
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

    @GetMapping("shedule/delete_ended_30_days")
    @Operation(summary = "Проверка удаления консультаций которые прошли 30 дней")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void testDeleteEnded30Days() {
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

    @PutMapping("users/{id}/role")
    @Operation(summary = "Изменение роли пользователя")
    @ResponseStatus(HttpStatus.OK)
    public void changeUserRole(
            @PathVariable Long id,
            @Valid Role newRole
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        try {
            if (newRole == Role.ADMIN) {
                throw new BadRequestException("Неверная роль. Доступные: STUDENT, TEACHER, DEANERY");
            }
            user.setRole(newRole);
            userRepository.save(user);
            log.info("Changed role for user #{} to {}", id, newRole);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Неверная роль. Доступные: STUDENT, TEACHER, DEANERY");
        }
    }

}
