package com.example.botforconsultations.core.exception;

// 401 - Unauthorized
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}