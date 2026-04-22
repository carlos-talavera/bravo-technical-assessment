package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataCreditApplicationRepository extends JpaRepository<CreditApplicationRow, UUID> {

    List<CreditApplicationRow> findByCountryAndStatus(CountryCode country, ApplicationStatus status);
}
