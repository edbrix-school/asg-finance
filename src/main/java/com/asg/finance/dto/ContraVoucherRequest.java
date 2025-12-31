package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherRequest {

    private Long transactionPoid;
    
    private LocalDate transactionDate;
    private String docRef;
    private String postingNarration;
    private String currencyCode;
    private BigDecimal amount;
    private BigDecimal bhdAmount;
    private BigDecimal drTotal;
    private BigDecimal crTotal;
    private String approvalStatus;
    private String createdBy;
    private Timestamp createdDate;
    private Long creditGl;
    private Long debitGl;
    
    // Additional optional fields
    private BigDecimal currencyRate;
    private String chequeNo;
    private String manual;
    
    private LocalDate chequeDate;
    
    private String multiCompany;
    private String oldJvno;
    private String remarks;
    private Long groupPoid;
    private Long companyPoid;
    private String deleted;
    
    // Detail lines
    private List<ContraVoucherDetailRequest> details = new ArrayList<>();
}

