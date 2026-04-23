package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataJobQueueRepository extends JpaRepository<JobQueueRow, UUID> {

    // Procesa hasta 5 trabajos por ejecución en esta instancia del worker.
    // SKIP LOCKED garantiza que instancias concurrentes no compitan por los mismos registros.
    @Query(value = """
            SELECT * FROM job_queue
            WHERE status = 'PENDING'
              AND job_type = 'RISK_EVALUATION'
            LIMIT 5
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<JobQueueRow> findPendingRiskEvaluationJobs();

    @Modifying
    @Query(value = """
            UPDATE job_queue
            SET status = 'DONE', attempts = attempts + 1, processed_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void markDone(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            UPDATE job_queue
            SET status = 'FAILED', attempts = attempts + 1, error_message = :error, processed_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    void markFailed(@Param("id") UUID id, @Param("error") String error);
}
