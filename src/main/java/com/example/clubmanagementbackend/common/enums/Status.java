package com.example.clubmanagementbackend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    ACTIVE,
    INACTIVE;

    @JsonCreator
    public static Status fromString(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return Status.valueOf(value.trim().toUpperCase());
    }
}

