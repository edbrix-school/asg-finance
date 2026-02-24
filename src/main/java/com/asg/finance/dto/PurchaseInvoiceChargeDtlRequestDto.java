package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseInvoiceChargeDtlRequestDto {

    private Long transactionPoid;
    private Long detRowId;

    private Long chargePoid;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;

    private Long fdaDetRowId;
    private String checkAll;

    private BigDecimal pdaAmount;
    private BigDecimal ffAmount;

    private String chargeFrom;

    private Long taxPoid;
    private Double taxPercentage;
    private BigDecimal taxAmount;

    private BigDecimal chargeBaseAmount;

    private Long supplierPoidFf;

    private String actionType;  // "isCreated", "isUpdated", "isDeleted", "noChanges"
}
