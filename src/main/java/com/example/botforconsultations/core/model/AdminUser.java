package com.example.botforconsultations.core.model;

import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser extends User {
    private String login;
    private String passwordHash;
}
