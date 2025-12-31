package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApPiFaDefaultDetailsDto {
    private String faDescription;
    private Long faCategoryPoid;
    private String assetType;
    private BigDecimal grossValue;
}
