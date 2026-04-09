package com.asg.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestStockDtlResponse {
    private Long transactionPoid;
    private Long detRowId;

    private Long stockPoid;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal baseAmount;

    private Long taxPoid;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal netSales;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}
