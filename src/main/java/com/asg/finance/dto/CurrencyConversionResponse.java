package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionResponse {
    private String currencyCode;
    private BigDecimal currencyRate;
    private BigDecimal amount;
    private BigDecimal bhdAmount;
    private LocalDate rateDate;
}
