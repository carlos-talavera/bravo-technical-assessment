package com.charlie2code.bravotechnicalassessment.presentation.dto;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditApplicationResponse(
        UUID id,
        String country,
        String fullName,
        String documentId,
        BigDecimal requestedAmount,
        BigDecimal monthlyIncome,
        String status,
        String bankName,
        String bankCurrency,
        Instant createdAt,
        Instant updatedAt
) {
    public static CreditApplicationResponse from(CreditApplication application) {
        return new CreditApplicationResponse(
                application.getId(),
                application.getCountry().name(),
                application.getFullName(),
                application.getDocumentId(),
                application.getRequestedAmount(),
                application.getMonthlyIncome(),
                application.getStatus().name(),
                application.getBankName(),
                application.getBankCurrency(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
