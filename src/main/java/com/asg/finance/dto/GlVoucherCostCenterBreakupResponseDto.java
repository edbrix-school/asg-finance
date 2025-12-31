package com.asg.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class GlVoucherCostCenterBreakupResponseDto {
    private List<CostCenterBreakupResponseDto> costBreakupList;
}
