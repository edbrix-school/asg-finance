package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPeriodStockDtlResponseDto {
    private Long detRowId;
    private Long stockPoid;
    private Long stockCatPoid;
    private Long outputTaxPoid;
    private Long inputTaxPoid;
    private String remarks;
    private DetailsDto stockPoidDet;
    private DetailsDto stockCatPoidDet;
    private DetailsDto outputTaxPoidDet;
    private DetailsDto inputTaxPoidDet;
}
