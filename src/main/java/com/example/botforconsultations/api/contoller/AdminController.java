package com.example.botforconsultations.api.contoller;

import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.api.mapper.UserMapper;
import com.example.botforconsultations.core.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("activate-account/{id}")
    @Operation(summary = "Активация аккаунта", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void activateAcc(
            @PathVariable Long id
    ) {
        adminService.activateAcc(id);
    }

    @GetMapping("deactivate-account/{id}")
    @Operation(summary = "Деактивация аккаунта", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void deactivateAcc(
            @PathVariable Long id
    ) {
        adminService.deactivateAcc(id);
    }

    @GetMapping("unactive-accounts")
    @Operation(summary = "Получения списка неактивных аккаунтов", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto.TelegramUserInfo> getUnactiveAccounts(
    ) {
        return userMapper.toTelegramUserInfo(adminService.getUnactiveAccounts());
    }

    @PostMapping("check-token")
    @Operation(summary = "Валидация токена")
    @ResponseStatus(HttpStatus.OK)
    public void checkToken(@RequestBody UserDto.Token token) {
        adminService.checkToken(token.token());
    }

    @GetMapping("user-info/{id}")
    @Operation(summary = "Получение информации о пользователе", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public UserDto.TelegramUserInfo getUserInfo(
            @PathVariable Long id
    ) {
        return userMapper.toTelegramUserInfo(adminService.getUserInfo(id));
    }
}
