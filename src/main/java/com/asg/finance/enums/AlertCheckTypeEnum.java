package com.asg.finance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertCheckTypeEnum {
    GENERAL("General"),
    DATECHECK("DateCheck");

    private final String value;

    AlertCheckTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AlertCheckTypeEnum fromJson(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (AlertCheckTypeEnum t : values()) {
            if (t.value.equalsIgnoreCase(input)
                    || t.name().equalsIgnoreCase(input)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid AlertCheckType " + input);

    }

    public static AlertCheckTypeEnum fromDbValue(String enumValue) {
        if (enumValue == null || enumValue.isBlank()) {
            return null;
        }
        for (AlertCheckTypeEnum t : values()) {
            if (t.value.equalsIgnoreCase(enumValue)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid Enum value for AlertCheckType: " + enumValue);
    }
}
