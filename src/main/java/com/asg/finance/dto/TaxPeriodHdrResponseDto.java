package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPeriodHdrResponseDto {
    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private String description;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    private List<TaxPeriodChargeDtlResponseDto> charges;
    private List<TaxPeriodStockDtlResponseDto> stocks;
}
