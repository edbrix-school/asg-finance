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
public class TaxPeriodChargeDtlResponseDto {
    private Long detRowId;
    private Long chargePoid;
    private Long chargeCatPoid;
    private Long outputTaxPoid;
    private Long inputTaxPoid;
    private String remarks;
    private DetailsDto chargePoidDet;
    private DetailsDto chargeCatPoidDet;
    private DetailsDto outputTaxPoidDet;
    private DetailsDto inputTaxPoidDet;
}
