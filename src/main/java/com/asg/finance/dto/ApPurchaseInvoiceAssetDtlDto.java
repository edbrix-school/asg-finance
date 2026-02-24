package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApPurchaseInvoiceAssetDtlDto {

    private Long transactionPoid;
    private Long detRowId;

    private String faCode;
    private String faDescription;
    private String faCategory;
    private String assetType;
    private BigDecimal value;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
