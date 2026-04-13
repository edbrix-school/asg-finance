package com.asg.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankReconciliationResponse {
    private Long transactionGroupPoid;
    private Long transactionCompanyPoid;
    private String docId;
    private String docId1;
    private String docTitle;
    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String chequeRef;
    private Long detRowId;
    private String narration;
    private Long glCompanyPoid;
    private Long glPoid;
    private BigDecimal crAmt;
    private BigDecimal drAmt;
}