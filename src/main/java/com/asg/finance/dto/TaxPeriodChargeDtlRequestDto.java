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
public class TaxPeriodChargeDtlRequestDto {
    @NotNull(message = "DET ROW ID is required")
    private Long detRowId;
    private Long chargePoid;
    private Long chargeCatPoid;
    private Long outputTaxPoid;
    private Long inputTaxPoid;
    private String remarks;
    private String action; // CREATE, EDIT, DELETE
}
