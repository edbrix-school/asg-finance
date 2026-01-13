package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationDetailResponse {

    private Long detRowId;
    private Long company;
    private String companyName;
    private BigDecimal sh;
    private BigDecimal ff;
    private BigDecimal ffs;
    private BigDecimal ffp;
    private BigDecimal properties;
    private BigDecimal mta;
    private BigDecimal pda;
    private BigDecimal admin;
    private BigDecimal total;
    private String remarks;
}

