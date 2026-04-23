package com.charlie2code.bravotechnicalassessment.application.usecase;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.port.ApplicationEventPublisher;
import com.charlie2code.bravotechnicalassessment.domain.port.AuditContext;
import com.charlie2code.bravotechnicalassessment.domain.port.WebhookNotifier;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UpdateApplicationStatusUseCase {

    private final CreditApplicationRepository repository;
    private final WebhookNotifier webhookNotifier;
    private final AuditContext auditContext;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateApplicationStatusUseCase(
            CreditApplicationRepository repository,
            WebhookNotifier webhookNotifier,
            AuditContext auditContext,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.webhookNotifier = webhookNotifier;
        this.auditContext = auditContext;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CreditApplication execute(UUID id, ApplicationStatus newStatus, String actor) {
        auditContext.set("API", actor);

        var application = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application not found: " + id));

        application.transitionTo(newStatus);

        var saved = repository.save(application);
        webhookNotifier.notify(saved);
        eventPublisher.publish(saved);
        return saved;
    }
}
