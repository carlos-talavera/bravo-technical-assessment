package com.charlie2code.bravotechnicalassessment.application.usecase;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCreditApplicationsUseCase {

    private final CreditApplicationRepository repository;

    public ListCreditApplicationsUseCase(CreditApplicationRepository repository) {
        this.repository = repository;
    }

    public List<CreditApplication> execute(CountryCode country, ApplicationStatus status) {
        return repository.findByFilters(country, status);
    }
}
