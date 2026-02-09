package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComputeTotalsResponse {

    private AllocationTotals totals;
    private BigDecimal grandTotal;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationTotals {
        private BigDecimal totalSh;
        private BigDecimal totalFf;
        private BigDecimal totalFfs;
        private BigDecimal totalFfp;
        private BigDecimal totalProperties;
        private BigDecimal totalMta;
        private BigDecimal totalPda;
        private BigDecimal totalAdmin;
    }
}

