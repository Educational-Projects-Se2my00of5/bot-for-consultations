package com.example.botforconsultations.core.util;

import com.example.botforconsultations.core.exception.NotFoundException;
import com.example.botforconsultations.core.model.AdminUser;
import com.example.botforconsultations.core.model.User;
import com.example.botforconsultations.core.repository.AdminUserRepository;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetModelOrThrow {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final TelegramUserRepository telegramUserRepository;


    public AdminUser getAdminByLogin(String login) {
        return adminUserRepository.findByLogin(login)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
