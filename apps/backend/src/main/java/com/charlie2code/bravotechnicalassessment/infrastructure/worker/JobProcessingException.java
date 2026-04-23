package com.charlie2code.bravotechnicalassessment.infrastructure.worker;

import java.util.UUID;

public class JobProcessingException extends RuntimeException {
    private final UUID jobId;

    public JobProcessingException(UUID jobId, Throwable cause) {
        super("Job " + jobId + " failed: " + cause.getMessage(), cause);
        this.jobId = jobId;
    }

    public UUID getJobId() {
        return jobId;
    }
}
