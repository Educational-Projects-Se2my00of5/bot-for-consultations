package com.example.botforconsultations.core.service;

import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.core.exception.AuthenticationException;
import com.example.botforconsultations.core.exception.BadRequestException;
import com.example.botforconsultations.core.exception.NotFoundException;
import com.example.botforconsultations.core.model.AdminUser;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.User;
import com.example.botforconsultations.core.repository.AdminUserRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import com.example.botforconsultations.core.util.GetModelOrThrow;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;
    private final TelegramUserRepository telegramUserRepository;

    private final GetModelOrThrow getModelOrThrow;

    public void activateAcc(Long id) {
        User user = getModelOrThrow.getUserById(id);

        if (user.getRole() == Role.TEACHER && user instanceof TelegramUser telegramUser) {
            telegramUser.setHasConfirmed(true);
            userRepository.save(telegramUser);
        } else {
            throw new BadRequestException("Пользователь не преподаватель");
        }
    }

    public void deactivateAcc(Long id) {
        User user = getModelOrThrow.getUserById(id);

        if (user.getRole() == Role.TEACHER && user instanceof TelegramUser telegramUser) {
            telegramUser.setHasConfirmed(false);
            userRepository.save(telegramUser);
        } else {
            throw new BadRequestException("Пользователь не преподаватель");
        }
    }

    public List<TelegramUser> getUnactiveAccounts() {
        return telegramUserRepository.findByRoleAndHasConfirmed(Role.TEACHER, false);
    }

    public String login(UserDto.Login request) {
        final AdminUser user = getModelOrThrow.getAdminByLogin(request.login());


        // сравниваем сырой и хэшированный пароль
        if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            final String token = jwtProvider.generateToken(user);

            return token;
        } else {
            throw new AuthenticationException("Неправильный пароль");
        }
    }

    public void checkToken(String token) {
        jwtProvider.validateToken(token);
    }

    public TelegramUser getUserInfo(Long id) {
        User user = getModelOrThrow.getUserById(id);

        if (user.getRole() == Role.TEACHER && user instanceof TelegramUser telegramUser) {
            return telegramUser;
        } else {
            throw new BadRequestException("Пользователь не преподаватель");
        }
    }
}
