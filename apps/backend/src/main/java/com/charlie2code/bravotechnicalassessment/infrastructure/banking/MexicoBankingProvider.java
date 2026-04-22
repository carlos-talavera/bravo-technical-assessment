package com.charlie2code.bravotechnicalassessment.infrastructure.banking;

import com.charlie2code.bravotechnicalassessment.domain.port.BankingProvider;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MexicoBankingProvider implements BankingProvider {

    @Override
    public CountryCode country() {
        return CountryCode.MX;
    }

    @Override
    public BankingInfo getInfo(String documentId) {
        return new BankingInfo(
                "014180012345678901",
                "Banamex",
                "MXN",
                new BigDecimal("50000.00"),
                720);
    }
}
