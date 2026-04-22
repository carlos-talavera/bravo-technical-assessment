package com.charlie2code.bravotechnicalassessment.infrastructure.config;

import com.charlie2code.bravotechnicalassessment.domain.policy.ColombiaCreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.policy.CreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.policy.MexicoCreditPolicy;
import com.charlie2code.bravotechnicalassessment.domain.port.BankingProvider;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class CountryStrategyConfig {

    @Bean
    public CreditPolicy mexicoCreditPolicy() {
        return new MexicoCreditPolicy();
    }

    @Bean
    public CreditPolicy colombiaCreditPolicy() {
        return new ColombiaCreditPolicy();
    }

    @Bean
    public Map<CountryCode, CreditPolicy> creditPolicies(List<CreditPolicy> policies) {
        return policies.stream().collect(Collectors.toMap(CreditPolicy::country, Function.identity()));
    }

    @Bean
    public Map<CountryCode, BankingProvider> bankingProviders(List<BankingProvider> providers) {
        return providers.stream().collect(Collectors.toMap(BankingProvider::country, Function.identity()));
    }
}
