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
public class InsuranceDetailResponseDto {
    private Long insuranceDetailPoid;
    private String details;
    private BigDecimal amount;
    private String remarks;
}