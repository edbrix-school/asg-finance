package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPeriodStockDtlRequestDto {
    @NotNull(message = "DET ROW ID is required")
    private Long detRowId;
    private Long stockPoid;
    private Long stockCatPoid;
    private Long outputTaxPoid;
    private Long inputTaxPoid;
    private String remarks;
    private String action; // CREATE, EDIT, DELETE
}
