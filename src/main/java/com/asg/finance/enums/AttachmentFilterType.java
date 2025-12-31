package com.asg.finance.enums;

public enum AttachmentFilterType {
    ACTIVE,    // ACTIVE = 'Y' AND DELETED = 'N'
    DELETED,   // DELETED = 'Y'
    ALL        // All attachments regardless of ACTIVE/DELETED status
}