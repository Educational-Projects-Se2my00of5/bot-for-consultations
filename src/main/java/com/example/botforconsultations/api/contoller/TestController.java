package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final TelegramUserRepository telegramUserRepository;

    @GetMapping("generate/teacher")
    @Operation(summary = "Генерация преподавателя")
    @ResponseStatus(HttpStatus.OK)
    public void generateTeacher(){
        TelegramUser telegramUser = TelegramUser
                .builder()
                .username("teacher")
                .fullName("teacher")
                .build();
        telegramUser.setRole(Role.TEACHER);
        telegramUserRepository.save(telegramUser);
    }


    @GetMapping("generate/student")
    @Operation(summary = "Генерация преподавателя")
    @ResponseStatus(HttpStatus.OK)
    public void generateStudent(){
        TelegramUser telegramUser = TelegramUser
                .builder()
                .username("student")
                .fullName("student")
                .build();
        telegramUser.setRole(Role.STUDENT);
        telegramUserRepository.save(telegramUser);
    }

}
