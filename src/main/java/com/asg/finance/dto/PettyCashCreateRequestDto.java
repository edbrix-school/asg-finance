package com.asg.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PettyCashCreateRequestDto implements PettyCashRequestBase {


    private LocalDate transactionDate;

    private String currencyCode;
    private BigDecimal currencyRate;

    @NotNull(message = "Petty Cash GL is required")
    private Long pettyCashGlPoid;

    private BigDecimal balance;

    // @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Paying To field is required")
    private String payingTo;

    @Size(max = 500, message = "Narration cannot exceed 500 characters")
    private String narration;

    private String advance;

    @NotNull(message = "Ref Type is required")
    private String refType;

    private String fdaRef;

    private String ffRef;


    private LocalDate settledDate;

    private String remarks;

    private BigDecimal settledTotal;

    // @NotNull(message = "Voucher Type is required")
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
    private List<GlPettyCashPaymentGrnDtlRequestDto> glPettyCashGrnDtlRequestDtos;
    private String docId;

    private Long bookPoid;
}

