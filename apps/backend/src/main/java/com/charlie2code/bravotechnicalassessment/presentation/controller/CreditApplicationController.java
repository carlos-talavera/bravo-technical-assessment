package com.charlie2code.bravotechnicalassessment.presentation.controller;

import com.charlie2code.bravotechnicalassessment.application.command.CreateApplicationCommand;
import com.charlie2code.bravotechnicalassessment.application.usecase.CreateCreditApplicationUseCase;
import com.charlie2code.bravotechnicalassessment.application.usecase.GetCreditApplicationUseCase;
import com.charlie2code.bravotechnicalassessment.application.usecase.ListCreditApplicationsUseCase;
import com.charlie2code.bravotechnicalassessment.application.usecase.UpdateApplicationStatusUseCase;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import com.charlie2code.bravotechnicalassessment.presentation.dto.CreditApplicationResponse;
import com.charlie2code.bravotechnicalassessment.presentation.dto.CreateCreditApplicationRequest;
import com.charlie2code.bravotechnicalassessment.presentation.dto.UpdateApplicationStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class CreditApplicationController {

    private final CreateCreditApplicationUseCase createUseCase;
    private final GetCreditApplicationUseCase getUseCase;
    private final ListCreditApplicationsUseCase listUseCase;
    private final UpdateApplicationStatusUseCase updateStatusUseCase;

    public CreditApplicationController(
            CreateCreditApplicationUseCase createUseCase,
            GetCreditApplicationUseCase getUseCase,
            ListCreditApplicationsUseCase listUseCase,
            UpdateApplicationStatusUseCase updateStatusUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.updateStatusUseCase = updateStatusUseCase;
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<CreditApplicationResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            Authentication authentication) {
        var application = updateStatusUseCase.execute(id, request.newStatus(), authentication.getName());
        return ResponseEntity.ok(CreditApplicationResponse.from(application));
    }
}
