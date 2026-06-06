package com.asg.finance.dto;

import com.asg.common.lib.dto.DetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PettyCashResponseDto {
    private Long transactionPoid;
    private LocalDate transactionDate;
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
    private DetailsDto fdaRefDtl;
    private String ffRef;
    private List<String> ffRefs;
    private DetailsDto ffRefDtl;
    private LocalDate settledDate;
    private String remarks;
    private BigDecimal settledTotal;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private String status;
    private BigDecimal grandTotal;
    private String mtaRef;
    private String multiCompany;
    private String poRef;
    private String salesQtnRef;
    private DetailsDto salesQtnRefDtl;
    private BigDecimal crTotal;
    private BigDecimal drTotal;
    private BigDecimal roundingAmount;
    private Long grnSupplierPoid;
    private DetailsDto grnSupplierPoidDtl;
    private Long supplierGlPoid;
    private DetailsDto supplierGlPoidDtl;
    private Long customerGlPoid;
    private DetailsDto customerGlPoidDtl;
    private Long advancePettyCashPoid;
    private DetailsDto advancePettyCashPoidDtl;
    private String advanceStatus;
    private BigDecimal advanceAmount;
    private Long companyDivPoid;

    // Child Tables
    private List<GlPettyCashPaymentDtlResponseDto> paymentDtls;
    private List<GlPettyCashChargeDtlResponseDto> chargeDtls;
    private List<GLPettyCashItemDtlResponseDto> itemDtls;
}
