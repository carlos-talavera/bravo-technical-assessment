package com.charlie2code.bravotechnicalassessment.infrastructure.sse;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.port.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class SseApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseApplicationEventPublisher.class);
    private static final String EVENT_NAME = "application-update";

    private final SseEmitterRegistry registry;

    public SseApplicationEventPublisher(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void publish(CreditApplication application) {
        try {
            registry.broadcast(EVENT_NAME, new ApplicationUpdateEvent(
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
                    application.getUpdatedAt()
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE event for application {}", application.getId(), e);
        }
    }

    record ApplicationUpdateEvent(
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
    ) {}
}
