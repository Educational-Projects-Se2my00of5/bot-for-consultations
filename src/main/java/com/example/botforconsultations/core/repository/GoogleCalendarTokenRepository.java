package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.GoogleCalendarToken;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleCalendarTokenRepository extends JpaRepository<GoogleCalendarToken, Long> {

    /**
     * Найти токен по пользователю
     */
    Optional<GoogleCalendarToken> findByUser(TelegramUser user);

    /**
     * Найти токен по ID пользователя
     */
    Optional<GoogleCalendarToken> findByUser_Id(Long userId);

    /**
     * Проверить, есть ли токен у пользователя
     */
    boolean existsByUser(TelegramUser user);

    /**
     * Удалить токен пользователя
     */
    void deleteByUser(TelegramUser user);
}
