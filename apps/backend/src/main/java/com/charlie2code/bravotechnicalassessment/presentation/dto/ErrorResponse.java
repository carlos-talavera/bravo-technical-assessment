package com.charlie2code.bravotechnicalassessment.presentation.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String message,
        int status,
        Map<String, String> errors,
        Instant timestamp
) {
    public ErrorResponse(String message, int status) {
        this(message, status, null, Instant.now());
    }

    public ErrorResponse(String message, int status, Map<String, String> errors) {
        this(message, status, errors, Instant.now());
    }
}
