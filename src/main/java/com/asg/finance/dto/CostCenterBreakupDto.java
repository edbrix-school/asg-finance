package com.asg.finance.dto;

import lombok.Data;

@Data
public class CostCenterBreakupDto {

    private Long companyPoid;
    private String costCenterCode;
    private String costCenterName;

    private Double amount;

    private String currency;
    private Double exchangeRate;

    private Long glDetRowId;
    private Long transactionPoid;

    private String remarks;
}
