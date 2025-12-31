package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpeningDetailsDto {

    private String openingAsset;
    private BigDecimal openingAssetValue;
    private BigDecimal accDepreciatedAmt;
    private BigDecimal wdvValue;
}
