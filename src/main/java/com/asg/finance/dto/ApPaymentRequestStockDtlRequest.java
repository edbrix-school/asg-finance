package com.asg.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestStockDtlRequest {
    private Long detRowId; // required for update

    private Long stockPoid;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal baseAmount;

    private Long taxPoid;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal netSales;
    private String actionType;
}
