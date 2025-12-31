package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.*;

@Getter
@Setter
public class BankDebitVoucherRequest {

    private Long transactionPoid;

    @NotNull(message = "Bank POID is required")
    private Long bankPoid;

    @NotBlank(message = "Currency is required")
    @Size(max = 20, message = "Currency must be 20 characters")
    private String currencyCode;

    @NotNull(message = "Currency rate is required")
    @DecimalMin(value = "0.0001", message = "Currency rate must be greater than 0")
    private BigDecimal currencyRate = BigDecimal.ONE;

    @NotNull(message = "Amount in currency is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Currency amount must be positive")
    private BigDecimal currencyAmt;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Narration is required")
    @Size(max = 2000, message = "Long narration must not exceed 2000 characters")
    private String longNarration;

    @NotBlank(message = "Paying Type is required")
    private String payingType;

    @NotBlank(message = "Reference Type is required")
    private String refType = "GENERAL";

    private Long payGlPoid;

    private Long beneficiaryBankPoid;

    private LocalDateTime ttDate;

    @DecimalMin(value = "0.00", message = "Gain/loss must be non-negative")
    private BigDecimal gainLoss;

    @Size(max = 20, message = "Gain/loss type must not exceed 20 characters")
    private String gainLossType;

    @DecimalMin(value = "0.00", message = "Bank charges must be non-negative")
    private BigDecimal bankCharges;

    private Long taxPoid;

    @DecimalMin(value = "0.00", message = "Tax percentage must be non-negative")
    private BigDecimal taxPercentage;

    @DecimalMin(value = "0.00", message = "Tax amount must be non-negative")
    private BigDecimal taxAmount;

    private String beneficiaryIban;

    @Size(max = 25, message = "Document reference must not exceed 25 characters")
    private String docRef;

    private Long fileUniqueId;

    private String fileName;

    private String fileGenerated;

    private LocalDateTime fileGeneratedDate;

    private String fileGeneratedBy;

    @Size(max = 25, message = "Paying To By must not exceed 25 characters")
    private String payingTo;

    private Date documentDate;

    private String ttChargeType;

    private Long bankPurposePoid;

    private BigDecimal ttSpecialRate;

    private String rateDealNo;

    private String ffRef;

    private Long fdaRef;

    private String payingToName;

    private String remarks;

    private String confidentialRemarks;

    private boolean suppressBalanceCheck;

    private List<PaymentGlDetails> paymentGlDetails;

    private List<ChargeDetailDto> chargeDetails;

    private List<TermsAndConditionDto> termsAndConditionDtoList;

}
