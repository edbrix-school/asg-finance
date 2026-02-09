package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationXlDetailResponse {

    private Long detRowId;
    private Long company;
    private String companyCode;
    private String companyName;
    private String costCentre;
    private BigDecimal percent;
    private String remarks;
}

