package com.charlie2code.bravotechnicalassessment.domain.repository;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditApplicationRepository {

    CreditApplication save(CreditApplication application);

    Optional<CreditApplication> findById(UUID id);

    List<CreditApplication> findByFilters(CountryCode country, ApplicationStatus status);
}
