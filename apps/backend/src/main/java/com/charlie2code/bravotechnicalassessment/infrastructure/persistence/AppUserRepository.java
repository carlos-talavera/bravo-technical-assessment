package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserRow, UUID> {

    Optional<AppUserRow> findByUsername(String username);
}
