package com.charlie2code.bravotechnicalassessment.infrastructure.banking;

import com.charlie2code.bravotechnicalassessment.domain.port.BankingProvider;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ColombiaBankingProvider implements BankingProvider {

    @Override
    public CountryCode country() {
        return CountryCode.CO;
    }

    @Override
    public BankingInfo getInfo(String documentId) {
        return new BankingInfo(
                "00113012345678",
                "Bancolombia",
                "COP",
                new BigDecimal("12000000.00"),
                680);
    }
}
