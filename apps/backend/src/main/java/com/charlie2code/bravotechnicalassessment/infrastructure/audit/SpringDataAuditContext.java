package com.charlie2code.bravotechnicalassessment.infrastructure.audit;

import com.charlie2code.bravotechnicalassessment.domain.port.AuditContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class SpringDataAuditContext implements AuditContext {

    private final EntityManager entityManager;

    public SpringDataAuditContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void set(String source, String changedBy) {
        entityManager.createNativeQuery("SELECT set_config('app.audit_source', :source, true)")
                .setParameter("source", source)
                .getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.changed_by', :changedBy, true)")
                .setParameter("changedBy", changedBy)
                .getSingleResult();
    }
}
