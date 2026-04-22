package com.charlie2code.bravotechnicalassessment.domain;

public class UnsupportedCountryException extends RuntimeException {

    public UnsupportedCountryException(CountryCode country) {
        super("Country not supported: " + country);
    }
}
