package com.asg.finance.enums.masters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum FrequencyTypeEnum {
    DAY("Day"), HOUR("Hour");

    private final String value;

    FrequencyTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }

    @JsonCreator
    public static FrequencyTypeEnum fromJson(String input) {
        if (StringUtils.isBlank(input)) {
            return DAY;
        }
        for (FrequencyTypeEnum t : values()) {
            if (t.value.equalsIgnoreCase(input)
                    || t.name().equalsIgnoreCase(input)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid FrequencyType " + input);
    }

    public static FrequencyTypeEnum fromDbValue(String dbData) {
        if (StringUtils.isBlank(dbData)) {
            return DAY;
        }
        return FrequencyTypeEnum.valueOf(dbData.toUpperCase());
    }
}