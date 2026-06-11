package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.ReconcileResultDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BankDebitVoucherResponse {
    private Long transactionPoid;
    private LocalDateTime transactionDate;
    private Long groupPoid;
    private LovGetListDto groupDet;
    private Long companyPoid;
    private LovGetListDto companyDet;

    private Long bankPoid;
    private LovGetListDto bankDet;
    private String currencyCode;
    private LovGetListDto currencyDet;

    private String docRef;
    private Long payGlPoid;
    private LovGetListDto payGlDet;
    private String payingTo;
    private String payingType;
    private BigDecimal amount;
    private String shortNarration;
    private String longNarration;
    private LocalDateTime ttDate;
    private BigDecimal currencyRate;
    private BigDecimal currencyAmt;
    private String refType;
    private String ffRef;
    private List<String> ffRefs;
    private List<LovGetListDto> ffRefsDtl;

    // Response-only: non-blocking warnings surfaced to the caller
    private List<String> warnings;
    // Response-only: success message from post-save job cost procedures
    private String jobCostMessage;
    private Long fdaRef;
    private String mtaRef;
    private String payingToName;
    private String remarks;
    private String ttChargeType;
    private Long salesQtnRef;
    private String beneficiaryIban;
    private Long beneficiaryBankPoid;
    private LovGetListDto beneficiaryBankDet;
    private BigDecimal taxAmount;
    private Long taxPoid;
    private LovGetListDto taxDet;
    private BigDecimal taxPercentage;
    private Long bankPurposePoid;
    private LovGetListDto bankPurposeDet;
    private String deleted;

    private List<PaymentGlDetails> paymentGlDetails;
    private List<ChargeDetailDto> chargeDetails;
    private List<ItemDetailDto> itemDetails;
    private List<TermsAndConditionDto> termsAndConditionDtoList;


    private BigDecimal gainLoss;
    private String gainLossType;
    private BigDecimal bankCharges;
    private BigDecimal ttSpecialRate;
    private String rateDealNo;
    private String beneficiaryBankName;
    private String confidentialRemarks;
    private String divisionCode;
    private BigDecimal bankBalance;
    private BigDecimal availableBalance;
    private String fileName;
    private String fileGenerated;
    private LocalDateTime fileGeneratedDate;
    private Long fileUniqueId;
    private String fileGeneratedBy;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    private ReconcileResultDto reconcileInfo;
    private String reconciledDateDisplay;
    private Boolean jobRefReadOnly;
}
