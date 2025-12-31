package com.asg.finance.enums;

public enum AssetDisposalProcess {
    SCRAP,
    SOLD,
    OBSOLETE;

    public static AssetDisposalProcess fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}
