package com.charlie2code.bravotechnicalassessment.domain.valueobject;

import java.math.BigDecimal;

public record BankingInfo(
        String accountNumber,
        String bankName,
        String currency,
        BigDecimal totalDebt,
        Integer creditScore
) {}
