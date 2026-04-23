package com.charlie2code.bravotechnicalassessment.infrastructure.worker;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.policy.CreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.port.AuditContext;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import com.charlie2code.bravotechnicalassessment.infrastructure.persistence.SpringDataJobQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class RiskEvaluationTxProcessor {

    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationTxProcessor.class);

    private final CreditApplicationRepository creditApplicationRepository;
    private final SpringDataJobQueueRepository jobQueueRepository;
    private final Map<CountryCode, CreditPolicy> policies;
    private final AuditContext auditContext;
    private final RiskEvaluationTxProcessor self;

    public RiskEvaluationTxProcessor(
            CreditApplicationRepository creditApplicationRepository,
            SpringDataJobQueueRepository jobQueueRepository,
            Map<CountryCode, CreditPolicy> policies,
            AuditContext auditContext,
            @Lazy RiskEvaluationTxProcessor self) {
        this.creditApplicationRepository = creditApplicationRepository;
        this.jobQueueRepository = jobQueueRepository;
        this.policies = policies;
        this.self = self;
        this.auditContext = auditContext;
    }

    public Optional<CreditApplication> processNext() {
        try {
            return self.doProcessNext();
        } catch (JobProcessingException e) {
            log.error("Job {} failed", e.getJobId(), e.getCause());
            self.markJobFailed(e.getJobId(),
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional
    public Optional<CreditApplication> doProcessNext() {
        auditContext.set("WORKER", "risk-worker");

        var maybeJob = jobQueueRepository.findNextPendingRiskEvaluationJob();
        if (maybeJob.isEmpty()) return Optional.empty();
        var job = maybeJob.get();

        try {
            var application = creditApplicationRepository.findById(job.getApplicationId())
                    .orElseThrow(() -> new IllegalStateException("Application not found"));
            var policy = policies.get(application.getCountry());
            if (policy == null) {
                throw new IllegalStateException("No policy for country " + application.getCountry());
            }
            var status = policy.evaluateRisk(application);
            application.transitionTo(status);
            var saved = creditApplicationRepository.save(application);
            jobQueueRepository.markDone(job.getId());

            log.info("Risk evaluation completed for application {} — status: {}",
                    application.getId(), application.getStatus());

            return Optional.of(saved);
        } catch (Exception e) {
            throw new JobProcessingException(job.getId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(UUID jobId, String message) {
        jobQueueRepository.markFailed(jobId, message);
    }
}
