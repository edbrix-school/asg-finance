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
public class InsuranceEmployeeDetailResponseDto {
    private Long employeeDetailPoid;
    private LovGetListDto employee;
    private BigDecimal amount;
    private String remarks;
}