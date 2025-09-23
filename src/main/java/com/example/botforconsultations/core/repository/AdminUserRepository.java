package com.example.botforconsultations.core.repository;

import com.example.botforconsultations.core.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    AdminUser findByLogin(String login);
}
