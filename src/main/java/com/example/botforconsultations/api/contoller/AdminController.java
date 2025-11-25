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

    // Эндпоинты для работы с деканатом
    @GetMapping("unactive-deanery-accounts")
    @Operation(summary = "Получение списка неактивных аккаунтов деканата", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto.TelegramUserInfo> getUnactiveDeaneryAccounts() {
        return userMapper.toTelegramUserInfo(adminService.getUnactiveDeaneryAccounts());
    }

    @GetMapping("activate-deanery-account/{id}")
    @Operation(summary = "Активация аккаунта деканата", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void activateDeaneryAccount(@PathVariable Long id) {
        adminService.activateDeaneryAccount(id);
    }

    @GetMapping("deactivate-deanery-account/{id}")
    @Operation(summary = "Деактивация аккаунта деканата", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void deactivateDeaneryAccount(@PathVariable Long id) {
        adminService.deactivateDeaneryAccount(id);
    }

    @GetMapping("deanery-user-info/{id}")
    @Operation(summary = "Получение информации о пользователе деканата", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public UserDto.TelegramUserInfo getDeaneryUserInfo(@PathVariable Long id) {
        return userMapper.toTelegramUserInfo(adminService.getDeaneryUserInfo(id));
    }
}

