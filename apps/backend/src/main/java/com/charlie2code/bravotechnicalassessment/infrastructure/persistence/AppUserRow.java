package com.charlie2code.bravotechnicalassessment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
@Access(AccessType.FIELD)
public class AppUserRow {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUserRow() {}

    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole()       { return role; }
}
