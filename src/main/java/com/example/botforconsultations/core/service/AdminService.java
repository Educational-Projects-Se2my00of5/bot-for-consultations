package com.example.botforconsultations.core.service;

import com.example.botforconsultations.api.bot.service.NotificationService;
import com.example.botforconsultations.api.dto.UserDto;
import com.example.botforconsultations.core.exception.AuthenticationException;
import com.example.botforconsultations.core.exception.BadRequestException;
import com.example.botforconsultations.core.model.AdminUser;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.User;
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

    private final NotificationService notificationService;

    private final UserRepository userRepository;
    private final TelegramUserRepository telegramUserRepository;

    private final GetModelOrThrow getModelOrThrow;

    // Универсальные методы для работы с пользователями

    /**
     * Получить всех неактивных пользователей (преподавателей и деканата)
     */
    public List<TelegramUser> getAllInactiveUsers() {
        return telegramUserRepository.findByHasConfirmed(false);
    }

    /**
     * Получить всех активных пользователей
     */
    public List<TelegramUser> getAllActiveUsers() {
        return telegramUserRepository.findByHasConfirmed(true);
    }

    /**
     * Активировать/деактивировать пользователя (универсальный метод)
     */
    public void toggleUserActivation(Long id, boolean activate) {
        User user = getModelOrThrow.getUserById(id);

        if (user instanceof TelegramUser telegramUser) {
            // Студентов нельзя деактивировать
            if (user.getRole() == Role.STUDENT) {
                throw new BadRequestException("Студентов нельзя деактивировать");
            }

            telegramUser.setHasConfirmed(activate);
            userRepository.save(telegramUser);

            if (activate) {
                if (telegramUser.getRole() == Role.TEACHER) {
                    notificationService.notifyTeacherAccountApproved(telegramUser.getTelegramId());
                } else if (telegramUser.getRole() == Role.DEANERY) {
                    notificationService.notifyDeaneryAccountApproved(telegramUser.getTelegramId());
                }
            }
        } else {
            throw new BadRequestException("Пользователь не найден");
        }
    }

    /**
     * Удалить пользователя
     */
    public void deleteUser(Long id) {
        User user = getModelOrThrow.getUserById(id);
        userRepository.delete(user);
    }

    /**
     * Обновить информацию о пользователе
     */
    public void updateUser(Long id, UserDto.UpdateUser updateDto) {
        User user = getModelOrThrow.getUserById(id);

        if (user instanceof TelegramUser telegramUser) {
            if (updateDto.firstName() != null) {
                telegramUser.setFirstName(updateDto.firstName());
            }
            if (updateDto.lastName() != null) {
                telegramUser.setLastName(updateDto.lastName());
            }

            userRepository.save(telegramUser);
        } else {
            throw new BadRequestException("Пользователь не найден");
        }
    }

    public String login(UserDto.Login request) {
        final AdminUser user = getModelOrThrow.getAdminByLogin(request.login());


        // сравниваем сырой и хэшированный пароль
        if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return jwtProvider.generateToken(user);
        } else {
            throw new AuthenticationException("Неправильный пароль");
        }
    }

    public void checkToken(String token) {
        jwtProvider.validateToken(token);
    }

    public TelegramUser getUserInfo(Long id) {
        User user = getModelOrThrow.getUserById(id);

        if (user instanceof TelegramUser telegramUser) {
            return telegramUser;
        } else {
            throw new BadRequestException("Пользователь не найден");
        }
    }
}