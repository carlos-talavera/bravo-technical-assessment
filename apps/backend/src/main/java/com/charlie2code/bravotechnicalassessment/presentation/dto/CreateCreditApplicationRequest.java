package com.charlie2code.bravotechnicalassessment.presentation.dto;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCreditApplicationRequest(

        @NotNull
        CountryCode country,

        @NotBlank
        @Size(max = 255)
        String fullName,

        @NotBlank
        @Size(max = 50)
        String documentId,

        @NotNull
        @Positive
        BigDecimal requestedAmount,

        @NotNull
        @Positive
        BigDecimal monthlyIncome
) {}
