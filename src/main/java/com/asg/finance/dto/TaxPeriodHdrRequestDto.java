package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPeriodHdrRequestDto {
    @NotBlank(message = "Description is mandatory")
    private String description;

    @NotNull(message = "Period From is mandatory")
    private LocalDate periodFrom;

    @NotNull(message = "Period To is mandatory")
    private LocalDate periodTo;

    private LocalDate transactionDate;

    private List<TaxPeriodChargeDtlRequestDto> charges;
    private List<TaxPeriodStockDtlRequestDto> stocks;

}
