package com.charlie2code.bravotechnicalassessment.domain.port;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.BankingInfo;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

public interface BankingProvider {

    CountryCode country();

    BankingInfo getInfo(String documentId);
}
