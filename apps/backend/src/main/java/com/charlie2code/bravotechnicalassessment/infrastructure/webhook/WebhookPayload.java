package com.charlie2code.bravotechnicalassessment.infrastructure.webhook;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;

import java.time.LocalDateTime;
import java.util.UUID;

public record WebhookPayload(
        UUID applicationId,
        String country,
        String status,
        String fullName,
        String documentId,
        LocalDateTime updatedAt
) {
    public static WebhookPayload from(CreditApplication application) {
        return new WebhookPayload(
                application.getId(),
                application.getCountry().name(),
                application.getStatus().name(),
                application.getFullName(),
                application.getDocumentId(),
                application.getUpdatedAt());
    }
}
