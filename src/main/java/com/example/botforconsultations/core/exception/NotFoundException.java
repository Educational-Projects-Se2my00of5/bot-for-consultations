package com.example.botforconsultations.core.exception;

// 404 - Not Found
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}