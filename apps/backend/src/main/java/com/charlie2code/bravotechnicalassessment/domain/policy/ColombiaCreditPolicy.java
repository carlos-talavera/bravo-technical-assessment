package com.charlie2code.bravotechnicalassessment.domain.policy;

import com.charlie2code.bravotechnicalassessment.domain.exception.ValidationException;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;

public class ColombiaCreditPolicy implements CreditPolicy {

    private static final String CC_REGEX = "^\\d{6,10}$";
    private static final int MAX_DEBT_MONTHS = 24;

    @Override
    public CountryCode country() {
        return CountryCode.CO;
    }

    @Override
    public void validateDocument(String documentId) {
        if (!documentId.matches(CC_REGEX)) {
            throw new ValidationException("Invalid Cédula de Ciudadanía format: " + documentId);
        }
    }

    @Override
    public void validateRules(BigDecimal requestedAmount, BigDecimal monthlyIncome, BankingInfo bankingInfo) {
        if (bankingInfo.debtExceedsMonthsOfIncome(monthlyIncome, MAX_DEBT_MONTHS)) {
            throw new ValidationException(
                    "Total debt exceeds " + MAX_DEBT_MONTHS + " months of monthly income for CO applicants");
        }
    }
}
