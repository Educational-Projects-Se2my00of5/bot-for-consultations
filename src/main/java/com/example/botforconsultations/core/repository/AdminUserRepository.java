package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByLogin(String login);
}
