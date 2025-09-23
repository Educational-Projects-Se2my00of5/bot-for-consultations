package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;

@Entity
public class AdminUser extends User {
    private String login;
    private String passwordHash;
}
