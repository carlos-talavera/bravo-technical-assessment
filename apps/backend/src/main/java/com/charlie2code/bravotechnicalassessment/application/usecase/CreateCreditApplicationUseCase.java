package com.charlie2code.bravotechnicalassessment.application.usecase;

import com.charlie2code.bravotechnicalassessment.application.command.CreateApplicationCommand;
import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.exception.UnsupportedCountryException;
import com.charlie2code.bravotechnicalassessment.domain.policy.CreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.port.ApplicationEventPublisher;
import com.charlie2code.bravotechnicalassessment.domain.port.BankingProvider;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CreateCreditApplicationUseCase {

    private final Map<CountryCode, CreditPolicy> policies;
    private final Map<CountryCode, BankingProvider> bankingProviders;
    private final CreditApplicationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateCreditApplicationUseCase(
            Map<CountryCode, CreditPolicy> policies,
            Map<CountryCode, BankingProvider> bankingProviders,
            CreditApplicationRepository repository,
            ApplicationEventPublisher eventPublisher) {
        this.policies = policies;
        this.bankingProviders = bankingProviders;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public CreditApplication execute(CreateApplicationCommand command) {
        var policy = Optional.ofNullable(policies.get(command.country()))
                .orElseThrow(() -> new UnsupportedCountryException(command.country()));

        var provider = Optional.ofNullable(bankingProviders.get(command.country()))
                .orElseThrow(() -> new UnsupportedCountryException(command.country()));

        policy.validateDocument(command.documentId());

        var bankingInfo = provider.getInfo(command.documentId());

        policy.validateRules(command.requestedAmount(), command.monthlyIncome(), bankingInfo);

        var application = CreditApplication.create(
                command.country(),
                command.fullName(),
                command.documentId(),
                command.requestedAmount(),
                command.monthlyIncome(),
                bankingInfo);

        var saved = repository.save(application);
        eventPublisher.publish(saved);
        return saved;
    }
}
