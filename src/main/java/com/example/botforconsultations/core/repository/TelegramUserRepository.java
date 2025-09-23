package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.Role;
import com.example.botforconsultations.core.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelegramUserRepository extends JpaRepository<TelegramUser,Long> {
    List<TelegramUser> findByRoleAndHasConfirmed(Role role, boolean hasConfirmed);
}
