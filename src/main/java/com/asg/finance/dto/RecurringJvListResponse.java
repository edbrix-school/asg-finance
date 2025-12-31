package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJvListResponse {

    private Long transactionPoid;
    private Timestamp transactionDate;
    private String docRef;
    private String narration;
    private String refType;
    private Timestamp startDate;
    private BigDecimal totalAmount;
    private Integer noOfMonths;
    private BigDecimal monthWiseAmt;
    private Long employeeId;
    private Long assetId;
    private String policyNumber;
    private String status;
    private String createdBy;
    private Timestamp createdDate;
}

