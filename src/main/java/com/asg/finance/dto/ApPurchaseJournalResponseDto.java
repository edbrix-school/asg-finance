package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApPurchaseJournalResponseDto {

    private Long chargePoid;
    private Double chargeBaseAmount;
    private Double ffAmount;
    private String refDocId;
    private String refDocPoid;
    private Long fdaDetRowId;
    private Long taxPoid;
    private Double taxPercentage;
    private Double taxAmount;
    private Double chargeAmount;
    private Double pdaAmount;
    private String remarks;
}
