package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComputeTotalsRequest {

    @Valid
    @NotNull(message = "Details are required")
    private List<AllocationDetail> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationDetail {
        private BigDecimal sh;
        private BigDecimal ff;
        private BigDecimal ffs;
        private BigDecimal ffp;
        private BigDecimal properties;
        private BigDecimal mta;
        private BigDecimal pda;
        private BigDecimal admin;
    }
}

