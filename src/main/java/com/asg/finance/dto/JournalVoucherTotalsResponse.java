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
public class JournalVoucherTotalsResponse {
    private BigDecimal drTotal;
    private BigDecimal crTotal;
    private BigDecimal difference;
    private Boolean isBalanced;
}
