package com.asg.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPaymentRequestDtlRequestDto {


    private Long detRowId;
    private Long chargePoid;

    private BigDecimal amount;
    private BigDecimal vatPer;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;

    private String remarks;
}
