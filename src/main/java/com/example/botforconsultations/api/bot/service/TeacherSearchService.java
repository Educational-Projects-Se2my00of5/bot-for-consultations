package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import com.example.botforconsultations.core.repository.TelegramUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для поиска преподавателей
 */
@Service
@RequiredArgsConstructor
public class TeacherSearchService {

    private final TelegramUserRepository telegramUserRepository;

    /**
     * Получить всех подтвержденных преподавателей
     */
    public List<TelegramUser> getAllTeachers() {
        return telegramUserRepository.findByRoleAndHasConfirmed(Role.TEACHER, true);
    }

    /**
     * Поиск преподавателей по имени или фамилии
     */
    public List<TelegramUser> searchTeachers(String query) {
        return telegramUserRepository
                .findByRoleAndHasConfirmedTrueAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        Role.TEACHER, query, query);
    }

    /**
     * Найти преподавателя по ID
     */
    public TelegramUser findById(Long id) {
        return telegramUserRepository.findById(id).orElse(null);
    }

    /**
     * Найти преподавателя по частям имени (из кнопки)
     * Формат: [Имя] или [Имя, Фамилия]
     */
    public TelegramUser findByNameParts(String[] nameParts) {
        if (nameParts == null || nameParts.length == 0) {
            return null;
        }
        
        if (nameParts.length >= 2) {
            // Есть имя и фамилия
            return telegramUserRepository.findByFirstNameAndLastNameAndRole(
                    nameParts[0].trim(), 
                    nameParts[1].trim(), 
                    Role.TEACHER
            ).orElse(null);
        } else {
            // Только имя (если нет фамилии)
            return telegramUserRepository.findByFirstNameAndRole(
                    nameParts[0].trim(), 
                    Role.TEACHER
            ).orElse(null);
        }
    }
}
