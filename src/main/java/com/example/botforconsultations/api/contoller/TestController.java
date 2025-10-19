package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.dto.UserGenerateDto;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
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

}
