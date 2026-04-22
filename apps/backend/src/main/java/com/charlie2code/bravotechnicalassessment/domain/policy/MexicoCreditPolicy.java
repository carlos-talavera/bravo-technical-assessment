package com.charlie2code.bravotechnicalassessment.domain.policy;

import com.charlie2code.bravotechnicalassessment.domain.exception.ValidationException;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MexicoCreditPolicy implements CreditPolicy {

    private static final String CURP_REGEX = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$";

    // Relación máxima entre monto solicitado e ingreso mensual.
    // Ej: ingreso de 10,000 MXN → préstamo máximo de 100,000 MXN.
    private static final BigDecimal MAX_LOAN_TO_INCOME_RATIO = BigDecimal.valueOf(10);

    @Override
    public CountryCode country() {
        return CountryCode.MX;
    }

    @Override
    public void validateDocument(String documentId) {
        if (!documentId.matches(CURP_REGEX)) {
            throw new ValidationException("Invalid CURP format: " + documentId);
        }
    }

    @Override
    public void validateRules(BigDecimal requestedAmount, BigDecimal monthlyIncome, BankingInfo bankingInfo) {
        BigDecimal ratio = requestedAmount.divide(monthlyIncome, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(MAX_LOAN_TO_INCOME_RATIO) > 0) {
            throw new ValidationException(
                    "Loan-to-income ratio exceeds the maximum allowed for MX applicants");
        }
    }
}
