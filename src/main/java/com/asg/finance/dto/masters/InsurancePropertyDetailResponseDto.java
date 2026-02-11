package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePropertyDetailResponseDto {
    private Long propertyDetailPoid;
    private LovGetListDto property;
    private BigDecimal amount;
    private String remarks;
}