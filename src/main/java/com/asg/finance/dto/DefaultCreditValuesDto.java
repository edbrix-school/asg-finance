package com.asg.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultCreditValuesDto {
    private Integer creditPeriod;
    private String tinNumber;
    private String currencyCode;
    private BigDecimal currencyRate;
    private Timestamp dueDate;
}