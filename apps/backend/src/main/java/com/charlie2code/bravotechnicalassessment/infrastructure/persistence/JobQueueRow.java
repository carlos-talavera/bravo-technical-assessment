package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_queue")
@Access(AccessType.FIELD)
public class JobQueueRow {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected JobQueueRow() {}

    public UUID getId()            { return id; }
    public UUID getApplicationId() { return applicationId; }
}
