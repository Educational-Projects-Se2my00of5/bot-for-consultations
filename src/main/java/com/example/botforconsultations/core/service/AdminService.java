package com.example.botforconsultations.core.service;

import com.example.botforconsultations.core.exception.BadRequestException;
import com.example.botforconsultations.core.exception.NotFoundException;
import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.model.User;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import com.example.botforconsultations.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TelegramUserRepository telegramUserRepository;

    public void activateAcc(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if(user.getRole() == Role.TEACHER && user instanceof TelegramUser telegramUser){
            telegramUser.setHasConfirmed(true);
            userRepository.save(telegramUser);
        } else {
            throw new BadRequestException("Пользователь не преподаватель");
        }
    }

    public void deactivateAcc(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if(user.getRole() == Role.TEACHER && user instanceof TelegramUser telegramUser){
            telegramUser.setHasConfirmed(false);
            userRepository.save(telegramUser);
        } else {
            throw new BadRequestException("Пользователь не преподаватель");
        }
    }

    public List<TelegramUser> getUnactiveAccounts() {
        return telegramUserRepository.findByRoleAndHasConfirmed(Role.TEACHER, false);
    }
}
