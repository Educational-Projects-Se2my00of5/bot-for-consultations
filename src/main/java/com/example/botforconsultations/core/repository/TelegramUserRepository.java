package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    List<TelegramUser> findByRoleAndHasConfirmed(Role role, boolean hasConfirmed);

    List<TelegramUser> findByHasConfirmed(boolean hasConfirmed);

    Optional<TelegramUser> findByTelegramId(Long telegramId);

    List<TelegramUser> findByRoleAndHasConfirmedTrueAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(Role role, String searchQuery, String searchQuery1);

    Optional<TelegramUser> findByFirstNameAndLastNameAndRole(String firstName, String lastName, Role role);

    Optional<TelegramUser> findByFirstNameAndRole(String firstName, Role role);
}
