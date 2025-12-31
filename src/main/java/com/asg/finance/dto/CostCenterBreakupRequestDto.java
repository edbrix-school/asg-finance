package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterBreakupRequestDto {

    private Long groupPoid;
    private Long companyPoid;
    private String docId;
    private Long transactionPoid;
    private Long mainDetRowId;
    private Long glPoid;
    private Long costDetRowId;
    private String costGroup;
    private String costPoid;
    private BigDecimal amount;
    private Long loginUserPoid;
}
