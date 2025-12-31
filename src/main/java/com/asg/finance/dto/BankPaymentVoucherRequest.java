package com.asg.finance.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BankPaymentVoucherRequest {

    private Long bankPoid;
    private Double amount;
    private String chequeDate;
    private String chequeNo;
    private String accountPayee;
    private String longNarration;
    private String refType;
    private Long payGlPoid;
    private String payingTo;
    private String remarks;
    private Long ffRefId;
    private Long fdaRefId;
    private String mtaRfqId;
    private String suppressValidation;
    private String multiple;
    private String securityCheque;
    private Boolean released;
    private String releasedToPerson;      // → RELEASED_TO_PERSON
    private String contact;
    private String chqPrintedUserCode;
    private LocalDate chqPrintedDate;
    private Long availableBalance;
    private List<BankPaymentGLDetailRequest> glDetails;
    private List<BankPaymentChargeDetailRequest> chargeDetailRequests;
    private List<BankPaymentItemDetailRequest> itemDetailRequests;
}