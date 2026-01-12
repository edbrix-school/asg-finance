package com.asg.finance.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ApPurchaseCnChargeDtlDto {
    private Long detRowId;
    private Long chargePoid;
    private BigDecimal chargeAmount;
    private String description;
    private String remarks;
    private String refDocId;
    private Long refDocPoid;
    private Long refDetRowId;
    private String checkAll;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal chargeBaseAmount;
    private String chargeFrom;
    private Long supplierPoidFf;
}
