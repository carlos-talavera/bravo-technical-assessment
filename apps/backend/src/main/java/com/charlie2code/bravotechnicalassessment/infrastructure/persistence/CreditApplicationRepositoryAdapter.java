package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CreditApplicationRepositoryAdapter implements CreditApplicationRepository {

    private final SpringDataCreditApplicationRepository repository;

    public CreditApplicationRepositoryAdapter(SpringDataCreditApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CreditApplication save(CreditApplication application) {
        return repository.save(CreditApplicationRow.fromDomain(application)).toDomain();
    }

    @Override
    public Optional<CreditApplication> findById(UUID id) {
        return repository.findById(id).map(CreditApplicationRow::toDomain);
    }

    @Override
    public List<CreditApplication> findByFilters(CountryCode country, ApplicationStatus status) {
        return repository.findByCountryAndStatus(country, status).stream()
                .map(CreditApplicationRow::toDomain)
                .toList();
    }
}
