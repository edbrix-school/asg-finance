package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxSubmissionDetailResponse {

    private Long detRowId;
    private String taxType;
    private Long taxPoid;
    private String taxCode;
    private String taxDescription;
    private String taxPercentage;
    private BigDecimal taxBaseAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;
}


