package com.charlie2code.bravotechnicalassessment.domain.exception;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;

public class UnsupportedCountryException extends RuntimeException {

    public UnsupportedCountryException(CountryCode country) {
        super("Country not supported: " + country);
    }
}
