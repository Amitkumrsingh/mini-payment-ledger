package com.example.paymentledger.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorResponse(String code, String message, String correlationId, Instant timestamp) {}
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> domain(DomainException e) { return response(e.status(), e.code(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        var field = e.getBindingResult().getFieldErrors().stream().findFirst();
        String message = field.map(x -> x.getField() + ": " + x.getDefaultMessage()).orElse("Invalid request");
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> constraint(DataIntegrityViolationException e) { return response(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION", "The request conflicts with existing data"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred"); }
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        String id = MDC.get("correlationId");
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, id, Instant.now()));
    }
}

