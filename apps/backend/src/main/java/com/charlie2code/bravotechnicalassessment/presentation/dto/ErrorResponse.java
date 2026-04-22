package com.charlie2code.bravotechnicalassessment.presentation.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String message,
        int status,
        Map<String, String> errors,
        LocalDateTime timestamp
) {
    public ErrorResponse(String message, int status) {
        this(message, status, null, LocalDateTime.now());
    }

    public ErrorResponse(String message, int status, Map<String, String> errors) {
        this(message, status, errors, LocalDateTime.now());
    }
}
