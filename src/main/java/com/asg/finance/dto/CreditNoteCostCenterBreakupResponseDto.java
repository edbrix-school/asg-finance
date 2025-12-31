package com.asg.finance.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreditNoteCostCenterBreakupResponseDto {

    private Long id;
    private Long glDetRowId;
    private String costCenterCode;
    private String costCenterName;
    private BigDecimal amount;
    private String remarks;
}
