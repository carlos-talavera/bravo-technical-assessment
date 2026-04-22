package com.charlie2code.bravotechnicalassessment.domain.valueobject;

public enum ApplicationStatus {
    PENDING, IN_REVIEW, APPROVED, REJECTED;

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case PENDING    -> next == IN_REVIEW || next == APPROVED || next == REJECTED;
            case IN_REVIEW  -> next == APPROVED  || next == REJECTED;
            case APPROVED, REJECTED -> false;
        };
    }
}
