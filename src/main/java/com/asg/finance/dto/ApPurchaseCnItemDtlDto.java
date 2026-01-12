package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ApPurchaseCnItemDtlDto {
    private Long detRowId;
    private Long stockPoid;
    private Long stockUnitPoid;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal total;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private String checkAll;
    private Long refDetRowId;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal amount;
    private BigDecimal baseAmount;
    
    // Action type field
    private String actionType; // isCreated, isUpdated, isDeleted
}
