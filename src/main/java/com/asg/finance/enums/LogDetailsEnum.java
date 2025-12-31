package com.asg.finance.enums;

public enum LogDetailsEnum {
    VIEWED("Viewed -"),
    CREATED("Created -"),
    MODIFIED("Modified -"),
    DEACTIVATED("Deactivated -"),
    DELETED("Deleted -"),
    APPROVED("Approved -"),
    REJECTED("Rejected -"),
    SUBMITTED("Submitted -"),
    CANCELLED("Cancelled -"),
    LOGIN("Login -"),
    LOGOUT("Logout -"),
    PASSWORD_RESET("Password Reset -"),
    PASSWORD_EMAIL_SENT("Password Email Sent -"),
    STATUS_CHANGED("Status Changed-");

    private final String description;

    LogDetailsEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}