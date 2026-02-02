package com.asg.finance.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BankPaymentVoucherResponse {

    // ===== Header Fields =====
    private Long transactionPoid;
    private LocalDate transactionDate;
    private Long groupPoid;
    private LovGetListDto groupDet;
    private Long companyPoid;
    private LovGetListDto companyDet;
    private String docRef;
    private Long payGlPoid;
    private LovGetListDto payGlDet;
    private String payingTo;
    private String payingType;
    private String divisionCode;
    private Long bankPoid;
    private LovGetListDto bankDet;
    private String chqCardNo;
    private LocalDate chqDate;
    private String currencyCode;
    private Long currencyRate;
    private BigDecimal currencyAmount;
    private Long localAmount;
    private String shortNarration;
    private String longNarration;
    private String chqPrinted;
    private String chequeIssuePhysical;
    private LocalDate releasedDate;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String deleted;
    private String prePrinted;
    private String multiCompany;
    private String released;
    private String releasedByUserCode;
    private String releasedPersonAddress;
    private String releasedPersonId;
    private Long releasedSeqNo;
    private String releasedToPerson;
    private Long fdaRef;
    private LovGetListDto fdaDet;
    private String ffRef;
    private LovGetListDto ffDet;
    private String mtaRef;
    private LovGetListDto mtaDet;
    private String poRef;
    private String refType;
    private String chqSignType;
    private String oldPvName;
    private String payToOldCode;
    private String accountPayee;
    private LocalDate reconciledDate;
    private String printWithoutBillwise;
    private String remarks;
    private String hold;
    private String suppressValidation;
    private String securityCheque;
    private String chqPrintedUserCode;
    private LocalDate chqPrintedDate;
    private Long availableBalance;

    // ===== Detail Sections =====
    private List<BankPaymentGLDetailResponse> glDetails;        // for GENERAL / FF JOBS / CUSTOM
    private List<BankPaymentChargeDetailResponse> chargeDetails; // for FDA JOBS
    private List<BankPaymentItemDetailResponse> itemDetails;     // for MTA RFQ
}
