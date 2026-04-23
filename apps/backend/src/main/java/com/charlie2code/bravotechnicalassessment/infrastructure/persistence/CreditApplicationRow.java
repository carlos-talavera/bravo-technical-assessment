package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_applications")
@Access(AccessType.FIELD)
public class CreditApplicationRow {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private CountryCode country;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "document_id", nullable = false, length = 50)
    private String documentId;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "bank_total_debt", precision = 15, scale = 2)
    private BigDecimal bankTotalDebt;

    @Column(name = "bank_credit_score")
    private Integer bankCreditScore;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_currency", length = 3)
    private String bankCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CreditApplicationRow() {}

    public static CreditApplicationRow fromDomain(CreditApplication domain) {
        var row = new CreditApplicationRow();
        row.id = domain.getId();
        row.country = domain.getCountry();
        row.fullName = domain.getFullName();
        row.documentId = domain.getDocumentId();
        row.requestedAmount = domain.getRequestedAmount();
        row.monthlyIncome = domain.getMonthlyIncome();
        row.status = domain.getStatus();
        row.bankAccountNumber = domain.getBankAccountNumber();
        row.bankTotalDebt = domain.getBankTotalDebt();
        row.bankCreditScore = domain.getBankCreditScore();
        row.bankName = domain.getBankName();
        row.bankCurrency = domain.getBankCurrency();
        row.createdAt = domain.getCreatedAt();
        row.updatedAt = domain.getUpdatedAt();
        return row;
    }

    public CreditApplication toDomain() {
        return CreditApplication.reconstitute(
                id, country, fullName, documentId,
                requestedAmount, monthlyIncome, status,
                bankAccountNumber, bankTotalDebt, bankCreditScore,
                bankName, bankCurrency, createdAt, updatedAt);
    }
}
