package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PettyCashUpdateRequestDto implements PettyCashRequestBase {

    @JsonFormat
    private Date transactionDate;

    private String currencyCode;
    private BigDecimal currencyRate;

    private Long pettyCashGlPoid;

    private BigDecimal balance;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero", groups = {})
    private BigDecimal amount;

    private String payingTo;

    @Size(max = 500, message = "Narration cannot exceed 500 characters")
    private String narration;

    private String advance;

    private String refType;

    private String fdaRef;

    private String ffRef;

    @JsonFormat
    private Date settledDate;

    private String remarks;

    private BigDecimal settledTotal;

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

    private List<GlPettyCashPaymentDtlRequestDto> glPettyCashPaymentDtlRequestDtos;
    private List<GlPettyCashItemDtlRequestDto> glPettyCashItemDtlRequestDtos;
    private List<GlPettyCashChargeDtlRequestDto> glPettyCashChargeDtlRequestDtos;
    private String docId;

    private Long bookPoid;
}

