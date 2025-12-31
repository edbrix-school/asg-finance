package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApPiFromPoResponseDto {
    private Long stockPoid;
    private Long stockUnitPoid;
    private BigDecimal poQty;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal baseAmount;
    private Long taxPoid;
    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal amount;
    private String remarks;

    private String refDocId;     // 200-101
    private String refDocPoid;   // PO POID(s)
    private Long refDetRowId;
}
