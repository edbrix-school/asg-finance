package com.asg.finance.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    CR("CR"),
    DR("DR");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    public static TransactionType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        return valueOf(value.toUpperCase());
    }
}
