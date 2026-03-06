package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContraVoucherFullResponse {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String postingNarration;
    private String deleted;
    private Long companyPoid;
    private PoidDetailsDto companyPoidDetails; // POID, CODE, DESCRIPTION from GLOBAL_COMPANY_MASTER
    private String currencyCode;
    private BigDecimal currencyRate;
    private BigDecimal amount;
    private BigDecimal bhdAmount;
    private BigDecimal drTotal;
    private BigDecimal crTotal;
    private String approvalStatus;
    private String createdBy;
    private LocalDateTime createdDate;
    private Long creditGl;
    private PoidDetailsDto creditGlDetails; // POID, CODE, DESCRIPTION from GL_MASTER
    private Long debitGl;
    private PoidDetailsDto debitGlDetails; // POID, CODE, DESCRIPTION from GL_MASTER
    private String chequeNo;
    private String manual;
    private LocalDate chequeDate;
    private String multiCompany;
    private String remarks;
    
    // Detail lines from GL_CONTRA_VOUCHER_DTL
    private List<ContraVoucherDetailResponse> details = new ArrayList<>();
}

