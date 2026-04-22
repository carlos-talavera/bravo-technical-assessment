package com.charlie2code.bravotechnicalassessment.presentation.controller;

import com.charlie2code.bravotechnicalassessment.application.command.CreateApplicationCommand;
import com.charlie2code.bravotechnicalassessment.application.usecase.CreateCreditApplicationUseCase;
import com.charlie2code.bravotechnicalassessment.application.usecase.GetCreditApplicationUseCase;
import com.charlie2code.bravotechnicalassessment.application.usecase.ListCreditApplicationsUseCase;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import com.charlie2code.bravotechnicalassessment.presentation.dto.CreditApplicationResponse;
import com.charlie2code.bravotechnicalassessment.presentation.dto.CreateCreditApplicationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class CreditApplicationController {

    private final CreateCreditApplicationUseCase createUseCase;
    private final GetCreditApplicationUseCase getUseCase;
    private final ListCreditApplicationsUseCase listUseCase;

    public CreditApplicationController(
            CreateCreditApplicationUseCase createUseCase,
            GetCreditApplicationUseCase getUseCase,
            ListCreditApplicationsUseCase listUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditApplicationResponse create(@Valid @RequestBody CreateCreditApplicationRequest request) {
        var command = new CreateApplicationCommand(
                request.country(),
                request.fullName(),
                request.documentId(),
                request.requestedAmount(),
                request.monthlyIncome());
        return CreditApplicationResponse.from(createUseCase.execute(command));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditApplicationResponse> findById(@PathVariable UUID id) {
        return getUseCase.execute(id)
                .map(CreditApplicationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<CreditApplicationResponse> findByFilters(
            @RequestParam CountryCode country,
            @RequestParam ApplicationStatus status) {
        return listUseCase.execute(country, status).stream()
                .map(CreditApplicationResponse::from)
                .toList();
    }
}
