package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApPurchaseInvoiceAssetDtlDto {

    private Long transactionPoid;
    private Long detRowId;

    private String faCode;
    private String faDescription;
    private String faCategory;
    private String assetType;
    private Long value;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
