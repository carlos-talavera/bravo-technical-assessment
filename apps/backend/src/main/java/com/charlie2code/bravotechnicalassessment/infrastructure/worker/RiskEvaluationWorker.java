package com.charlie2code.bravotechnicalassessment.infrastructure.worker;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.port.ApplicationEventPublisher;
import com.charlie2code.bravotechnicalassessment.domain.port.WebhookNotifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RiskEvaluationWorker {

    private final RiskEvaluationTxProcessor txProcessor;
    private final WebhookNotifier webhookNotifier;
    private final ApplicationEventPublisher eventPublisher;

    public RiskEvaluationWorker(
            RiskEvaluationTxProcessor txProcessor,
            WebhookNotifier webhookNotifier,
            ApplicationEventPublisher eventPublisher) {
        this.txProcessor = txProcessor;
        this.webhookNotifier = webhookNotifier;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void run() {
        for (int i = 0; i < 5; i++) {
            Optional<CreditApplication> result = txProcessor.processNext();
            if (result.isEmpty()) break;
            webhookNotifier.notify(result.get());
            eventPublisher.publish(result.get());
        }
    }
}
