package com.charlie2code.bravotechnicalassessment.domain.valueobject;

public enum ApplicationStatus {
    PENDING, UNDER_REVIEW, APPROVED, REJECTED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case PENDING      -> next == UNDER_REVIEW || next == APPROVED || next == REJECTED;
            case UNDER_REVIEW -> next == APPROVED     || next == REJECTED;
            case APPROVED, REJECTED -> false;
        };
    }
}
