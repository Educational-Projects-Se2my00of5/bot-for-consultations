package com.example.botforconsultations.api.bot.service;

import com.example.botforconsultations.api.bot.utils.TeacherNameFormatter;
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
     * Проверяет, что пользователь является подтвержденным преподавателем
     */
    public TelegramUser findById(Long id) {
        return telegramUserRepository.findById(id)
                .filter(user -> user.getRole() == Role.TEACHER && user.isHasConfirmed())
                .orElse(null);
    }

    /**
     * Найти преподавателя по ID из кнопки
     * Формат кнопки: "👨‍🏫 №123 Имя Фамилия"
     * Проверяет, что пользователь является подтвержденным преподавателем
     */
    public TelegramUser findByIdFromButton(String teacherButton) {
        Long teacherId = TeacherNameFormatter.extractTeacherId(teacherButton);
        if (teacherId == null) {
            return null;
        }
        return findById(teacherId);
    }
}
