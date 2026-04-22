package com.charlie2code.bravotechnicalassessment.application.command;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;

public record CreateApplicationCommand(
        CountryCode country,
        String fullName,
        String documentId,
        BigDecimal requestedAmount,
        BigDecimal monthlyIncome
) {}
