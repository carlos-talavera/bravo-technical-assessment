package com.charlie2code.bravotechnicalassessment.domain.port;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;

public interface ApplicationEventPublisher {
    void publish(CreditApplication application);
}
