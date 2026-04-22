package com.charlie2code.bravotechnicalassessment.application.usecase;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class GetCreditApplicationUseCase {

    private final CreditApplicationRepository repository;

    public GetCreditApplicationUseCase(CreditApplicationRepository repository) {
        this.repository = repository;
    }

    public Optional<CreditApplication> execute(UUID id) {
        return repository.findById(id);
    }
}
