package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.api.mapper.UserMapper;
import com.example.botforconsultations.core.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper userMapper;

    @PostMapping("login")
    @Operation(summary = "Вход в аккаунт")
    @ResponseStatus(HttpStatus.OK)
    public String login(
            @Valid @RequestBody UserDto.Login request
    ) {
        return adminService.login(request);
    }

    @PostMapping("check-token")
    @Operation(summary = "Валидация токена")
    @ResponseStatus(HttpStatus.OK)
    public void checkToken(@RequestBody UserDto.Token token) {
        adminService.checkToken(token.token());
    }

    // ========== Универсальные эндпоинты для работы с пользователями ==========

    @GetMapping("users/inactive")
    @Operation(summary = "Получение всех неактивных пользователей", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto.TelegramUserInfo> getAllInactiveUsers() {
        return userMapper.toTelegramUserInfo(adminService.getAllInactiveUsers());
    }

    @GetMapping("users/active")
    @Operation(summary = "Получение всех активных пользователей", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto.TelegramUserInfo> getAllActiveUsers() {
        return userMapper.toTelegramUserInfo(adminService.getAllActiveUsers());
    }

    @GetMapping("users/{id}")
    @Operation(summary = "Получение информации о пользователе", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public UserDto.TelegramUserInfo getUserInfo(@PathVariable Long id) {
        return userMapper.toTelegramUserInfo(adminService.getUserInfo(id));
    }

    @PutMapping("users/{id}/activate")
    @Operation(summary = "Активация пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void activateUser(@PathVariable Long id) {
        adminService.toggleUserActivation(id, true);
    }

    @PutMapping("users/{id}/deactivate")
    @Operation(summary = "Деактивация пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void deactivateUser(@PathVariable Long id) {
        adminService.toggleUserActivation(id, false);
    }

    @PutMapping("users/{id}")
    @Operation(summary = "Обновление информации о пользователе", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateUser updateDto
    ) {
        adminService.updateUser(id, updateDto);
    }

    @DeleteMapping("users/{id}")
    @Operation(summary = "Удаление пользователя", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
    }
}

