package com.charlie2code.bravotechnicalassessment.presentation.dto;

import com.charlie2code.bravotechnicalassessment.domain.valueobject.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "newStatus is required")
        ApplicationStatus newStatus
) {}
