package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import com.charlie2code.bravotechnicalassessment.domain.entity.CreditApplication;
import com.charlie2code.bravotechnicalassessment.domain.repository.CreditApplicationRepository;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import com.charlie2code.bravotechnicalassessment.domain.valueobject.CountryCode;
import com.charlie2code.bravotechnicalassessment.infrastructure.cache.CreditApplicationCacheDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CreditApplicationRepositoryAdapter implements CreditApplicationRepository {

    private static final Logger log = LoggerFactory.getLogger(CreditApplicationRepositoryAdapter.class);
    private static final String ID_KEY_PREFIX = "credit-application::";
    private static final Duration TTL = Duration.ofDays(1);

    private final SpringDataCreditApplicationRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CreditApplicationRepositoryAdapter(
            SpringDataCreditApplicationRepository repository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CreditApplication save(CreditApplication application) {
        CreditApplication saved = repository.save(CreditApplicationRow.fromDomain(application)).toDomain();
        putInIdCache(saved);
        return saved;
    }

    @Override
    public Optional<CreditApplication> findById(UUID id) {
        String key = ID_KEY_PREFIX + id;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return Optional.of(objectMapper.readValue(json, CreditApplicationCacheDto.class).toDomain());
            } catch (JacksonException e) {
                log.warn("Cache read failed for id={}, falling back to DB", id, e);
            }
        }

        return repository.findById(id)
                .map(CreditApplicationRow::toDomain)
                .map(app -> {
                    putInIdCache(app);
                    return app;
                });
    }

    @Override
    public List<CreditApplication> findByFilters(CountryCode country, ApplicationStatus status) {
        return repository.findByCountryAndStatus(country, status).stream()
                .map(CreditApplicationRow::toDomain)
                .toList();
    }

    private void putInIdCache(CreditApplication app) {
        try {
            String json = objectMapper.writeValueAsString(CreditApplicationCacheDto.from(app));
            redisTemplate.opsForValue().set(ID_KEY_PREFIX + app.getId(), json, TTL);
        } catch (JacksonException e) {
            log.warn("Cache write failed for id={}", app.getId(), e);
        }
    }
}
