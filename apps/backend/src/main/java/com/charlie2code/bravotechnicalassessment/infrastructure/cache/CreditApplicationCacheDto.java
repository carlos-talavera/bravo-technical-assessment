package com.charlie2code.bravotechnicalassessment.infrastructure.cache;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditApplicationCacheDto(
        UUID id,
        String country,
        String fullName,
        String documentId,
        BigDecimal requestedAmount,
        BigDecimal monthlyIncome,
        String status,
        String bankAccountNumber,
        BigDecimal bankTotalDebt,
        Integer bankCreditScore,
        String bankName,
        String bankCurrency,
        Instant createdAt,
        Instant updatedAt
) {
    public static CreditApplicationCacheDto from(CreditApplication app) {
        return new CreditApplicationCacheDto(
                app.getId(),
                app.getCountry().name(),
                app.getFullName(),
                app.getDocumentId(),
                app.getRequestedAmount(),
                app.getMonthlyIncome(),
                app.getStatus().name(),
                app.getBankAccountNumber(),
                app.getBankTotalDebt(),
                app.getBankCreditScore(),
                app.getBankName(),
                app.getBankCurrency(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    public CreditApplication toDomain() {
        return CreditApplication.reconstitute(
                id,
                CountryCode.valueOf(country),
                fullName,
                documentId,
                requestedAmount,
                monthlyIncome,
                ApplicationStatus.valueOf(status),
                bankAccountNumber,
                bankTotalDebt,
                bankCreditScore,
                bankName,
                bankCurrency,
                createdAt,
                updatedAt
        );
    }
}
