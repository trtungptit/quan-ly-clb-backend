package com.example.clubmanagementbackend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RegistrationStatus {
    PENDING, APPROVED, REJECTED, CANCELLED, JOINED;

    @JsonCreator
    public static RegistrationStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return RegistrationStatus.valueOf(value.trim().toUpperCase());
    }
}
