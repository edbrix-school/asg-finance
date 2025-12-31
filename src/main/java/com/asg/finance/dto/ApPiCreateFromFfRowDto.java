package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApPiCreateFromFfRowDto {
    private Long chargePoid;
    private BigDecimal chargeBaseAmount;
    private BigDecimal ffAmount;
    private String refDocId;
    private Long refDocPoid; // FF job no
    private Long fdaDetRowId;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal chargeAmount; // base + tax
}
