package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PettyCashResponseDto {
    private Long transactionPoid;
    private Date transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private String currencyCode;
    private BigDecimal currencyRate;
    private Long pettyCashGlPoid;
    private DetailsDto pettyCashGlPoidDtl;
    private BigDecimal balance;
    private BigDecimal amount;
    private String payingTo;
    private String narration;
    private String advance;
    private String refType;
    private String fdaRef;
    private String ffRef;
    private Date settledDate;
    private String remarks;
    private BigDecimal settledTotal;
    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
    private String deleted;
    private String status;
    private BigDecimal grandTotal;
    private String mtaRef;
    private String multiCompany;
    private String poRef;
    private String salesQtnRef;
    private BigDecimal crTotal;
    private BigDecimal drTotal;
    private BigDecimal roundingAmount;
    private Long grnSupplierPoid;
    private Long supplierGlPoid;
    private Long customerGlPoid;
    private Long advancePettyCashPoid;
    private String advanceStatus;
    private BigDecimal advanceAmount;
    private Long companyDivPoid;

    // Child Tables
    private List<GlPettyCashPaymentDtlResponseDto> paymentDtls;
    private List<GlPettyCashChargeDtlResponseDto> chargeDtls;
    private List<GLPettyCashItemDtlResponseDto> itemDtls;
}
