package com.charlie2code.bravotechnicalassessment.infrastructure.worker;

import com.charlie2code.bravotechnicalassessment.domain.policy.CreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.port.AuditContext;
import com.charlie2code.bravotechnicalassessment.domain.port.WebhookNotifier;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import com.charlie2code.bravotechnicalassessment.infrastructure.persistence.JobQueueRow;
import com.charlie2code.bravotechnicalassessment.infrastructure.persistence.SpringDataJobQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class RiskEvaluationWorker {

    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationWorker.class);

    private final SpringDataJobQueueRepository jobQueueRepository;
    private final CreditApplicationRepository creditApplicationRepository;
    private final WebhookNotifier webhookNotifier;
    private final Map<CountryCode, CreditPolicy> policies;
    private final AuditContext auditContext;

    public RiskEvaluationWorker(
            SpringDataJobQueueRepository jobQueueRepository,
            CreditApplicationRepository creditApplicationRepository,
            WebhookNotifier webhookNotifier,
            Map<CountryCode, CreditPolicy> policies,
            AuditContext auditContext) {
        this.jobQueueRepository = jobQueueRepository;
        this.creditApplicationRepository = creditApplicationRepository;
        this.webhookNotifier = webhookNotifier;
        this.policies = policies;
        this.auditContext = auditContext;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void run() {
        auditContext.set("WORKER", "risk-worker");
        jobQueueRepository.findPendingRiskEvaluationJobs().forEach(this::processJob);
    }

    private void processJob(JobQueueRow job) {
        try {
            var application = creditApplicationRepository.findById(job.getApplicationId())
                    .orElseThrow(() -> new IllegalStateException("Application not found: " + job.getApplicationId()));

            var policy = policies.get(application.getCountry());
            application.transitionTo(policy.evaluateRisk(application));
            creditApplicationRepository.save(application);
            webhookNotifier.notify(application);
            jobQueueRepository.markDone(job.getId());

            log.info("Risk evaluation completed for application {} — status: {}",
                    application.getId(), application.getStatus());
        } catch (Exception e) {
            log.error("Risk evaluation failed for job {}: {}", job.getId(), e.getMessage());
            try {
                jobQueueRepository.markFailed(job.getId(), e.getMessage());
            } catch (Exception ex) {
                log.error("Could not mark job {} as failed: {}", job.getId(), ex.getMessage());
            }
        }
    }
}
