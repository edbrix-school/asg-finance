package com.asg.finance.enums.reports;

public enum ParameterUpdateStatus {
    SUCCESS, FAILED;

    public static ParameterUpdateStatus fromString(String status) {
        if (status == null) return FAILED;
        try {
            return ParameterUpdateStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FAILED;
        }
    }
}
