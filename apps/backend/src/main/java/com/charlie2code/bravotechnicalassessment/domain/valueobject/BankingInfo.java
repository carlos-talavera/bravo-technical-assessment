package com.charlie2code.bravotechnicalassessment.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record BankingInfo(
        String accountNumber,
        String bankName,
        String currency,
        BigDecimal totalDebt,
        Integer creditScore
) {
    public boolean debtExceedsMonthsOfIncome(BigDecimal monthlyIncome, int maxMonths) {
        if (totalDebt == null || monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal ratio = totalDebt.divide(monthlyIncome, 2, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.valueOf(maxMonths)) > 0;
    }
}
