package com.charlie2code.bravotechnicalassessment.domain.policy;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;

public interface CreditPolicy {

    CountryCode country();

    void validateDocument(String documentId);

    void validateRules(BigDecimal requestedAmount, BigDecimal monthlyIncome, BankingInfo bankingInfo);

    ApplicationStatus evaluateRisk(CreditApplication application);
}
