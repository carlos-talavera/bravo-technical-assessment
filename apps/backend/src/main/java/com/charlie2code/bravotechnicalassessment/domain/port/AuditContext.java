package com.charlie2code.bravotechnicalassessment.domain.port;

public interface AuditContext {
    void set(String source, String changedBy);
}
