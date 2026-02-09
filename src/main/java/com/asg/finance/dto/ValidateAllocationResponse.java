package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAllocationResponse {

    private Boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private AllocationTotals totals;

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
        private BigDecimal grandTotal;
    }
}

