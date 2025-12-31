package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitNoteChargeDetailDto {
    private Long transactionPoid;
    
    private Long detRowId;
    
    private Long chargeId; // CHARGE_MASTER_IN_DN_FOR_SH
    
    private BigDecimal costAmount;
    
    private BigDecimal chargeAmount;
    
    private Long taxId; // DR_TAX_MASTER
    
    private BigDecimal taxPercentage;
    
    private BigDecimal taxAmount;
    
    private String remarks;
    
    private Integer seqNo;
    
    private Boolean selected = false;
}