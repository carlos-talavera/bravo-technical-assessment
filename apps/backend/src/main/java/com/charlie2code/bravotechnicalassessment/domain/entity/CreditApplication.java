package com.charlie2code.bravotechnicalassessment.domain.entity;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreditApplication {

    private final UUID id;
    private final CountryCode country;
    private final String fullName;
    private final String documentId;
    private final BigDecimal requestedAmount;
    private final BigDecimal monthlyIncome;
    private ApplicationStatus status;
    private final String bankAccountNumber;
    private final BigDecimal bankTotalDebt;
    private final Integer bankCreditScore;
    private final String bankName;
    private final String bankCurrency;
    private final Instant createdAt;
    private Instant updatedAt;

    private CreditApplication(
            UUID id,
            CountryCode country,
            String fullName,
            String documentId,
            BigDecimal requestedAmount,
            BigDecimal monthlyIncome,
            ApplicationStatus status,
            String bankAccountNumber,
            BigDecimal bankTotalDebt,
            Integer bankCreditScore,
            String bankName,
            String bankCurrency,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.country = country;
        this.fullName = fullName;
        this.documentId = documentId;
        this.requestedAmount = requestedAmount;
        this.monthlyIncome = monthlyIncome;
        this.status = status;
        this.bankAccountNumber = bankAccountNumber;
        this.bankTotalDebt = bankTotalDebt;
        this.bankCreditScore = bankCreditScore;
        this.bankName = bankName;
        this.bankCurrency = bankCurrency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CreditApplication create(
            CountryCode country,
            String fullName,
            String documentId,
            BigDecimal requestedAmount,
            BigDecimal monthlyIncome,
            BankingInfo bankingInfo) {

        var now = Instant.now();
        return new CreditApplication(
                UUID.randomUUID(),
                country,
                fullName,
                documentId,
                requestedAmount,
                monthlyIncome,
                ApplicationStatus.PENDING,
                bankingInfo.accountNumber(),
                bankingInfo.totalDebt(),
                bankingInfo.creditScore(),
                bankingInfo.bankName(),
                bankingInfo.currency(),
                now,
                now);
    }

    // Reconstruye la entidad desde persistencia sin imponer restricciones de estado inicial
    public static CreditApplication reconstitute(
            UUID id,
            CountryCode country,
            String fullName,
            String documentId,
            BigDecimal requestedAmount,
            BigDecimal monthlyIncome,
            ApplicationStatus status,
            String bankAccountNumber,
            BigDecimal bankTotalDebt,
            Integer bankCreditScore,
            String bankName,
            String bankCurrency,
            Instant createdAt,
            Instant updatedAt) {

        return new CreditApplication(
                id, country, fullName, documentId,
                requestedAmount, monthlyIncome, status,
                bankAccountNumber, bankTotalDebt, bankCreditScore,
                bankName, bankCurrency, createdAt, updatedAt);
    }

    public void transitionTo(ApplicationStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId()                    { return id; }
    public CountryCode getCountry()        { return country; }
    public String getFullName()            { return fullName; }
    public String getDocumentId()          { return documentId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getMonthlyIncome()   { return monthlyIncome; }
    public ApplicationStatus getStatus()   { return status; }
    public String getBankAccountNumber()   { return bankAccountNumber; }
    public BigDecimal getBankTotalDebt()   { return bankTotalDebt; }
    public Integer getBankCreditScore()    { return bankCreditScore; }
    public String getBankName()            { return bankName; }
    public String getBankCurrency()        { return bankCurrency; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
}
