package com.example.botforconsultations.api.error;


import com.example.botforconsultations.api.dto.SimpleErrorResponseDto;
import com.example.botforconsultations.core.exception.AuthenticationException;
import com.example.botforconsultations.core.exception.BadRequestException;
import com.example.botforconsultations.core.exception.ForbiddenException;
import com.example.botforconsultations.core.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleAuthException(AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleForbiddenException(ForbiddenException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleNotFound(NotFoundException ex) {

        log.info("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleBadRequest(BadRequestException ex) {

        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed for request. Errors: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    // ошибка для несуществующих путей
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleNotFound(NoResourceFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    // ошибка для пустого заголовка Authorization
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleNotFound(MissingRequestHeaderException ex) {

        log.warn("Missing Header: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }

    // для поломанных или вообще отсутствующих JSON в теле запроса
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.warn("Bad Request: Malformed JSON or invalid data format. Details: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto("Invalid request body format. Please check your JSON."));
    }

    // для неправильного типа значения в параметрах запроса
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleInvalidJson(MethodArgumentTypeMismatchException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto("Method parameter type invalid. Please check your request URL."));
    }

    // для неправильного значения в параметрах запроса
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleInvalidJson(PropertyReferenceException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto(ex.getMessage()));
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<SimpleErrorResponseDto> handleInvalidJson(DataIntegrityViolationException ex) {
        log.warn("Bad Request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SimpleErrorResponseDto(
                        ex.getMessage().contains("unique constraint") ? ex.getMessage() : "Invalid Request. There is already such an object"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SimpleErrorResponseDto> handleOtherExceptions(Exception ex) {
        log.error("An unexpected internal server error occurred", ex);

        String friendlyMessage = "Internal server error. Please contact support.";
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SimpleErrorResponseDto(friendlyMessage));
    }
}