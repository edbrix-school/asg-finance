package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "GL_BANK_DEBIT_HDR")
public class GlBankDebitHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", nullable = false, length = 25)
    private String docRef;

    @Column(name = "PAY_GL_POID")
    private Long payGlPoid;

    @Column(name = "PAYING_TO", length = 100)
    private String payingTo;

    @Column(name = "PAYING_TYPE", length = 25)
    private String payingType;

    @Column(name = "DIVISION_CODE", length = 25)
    private String divisionCode;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "SHORT_NARRATION", length = 200)
    private String shortNarration;

    @Column(name = "LONG_NARRATION", length = 2000)
    private String longNarration;

    @Column(name = "BANK_BALANCE")
    private BigDecimal bankBalance;

    @Column(name = "AVAILABLE_BALANCE")
    private BigDecimal availableBalance;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "TT_DATE")
    private LocalDateTime ttDate;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "TT_REMARK_POID")
    private Long ttRemarkPoid;

    @Column(name = "CURRENCY_AMT")
    private BigDecimal currencyAmt;

    @Column(name = "BANK_CHARGES")
    private BigDecimal bankCharges;

    @Column(name = "GAIN_LOSS")
    private BigDecimal gainLoss;

    @Column(name = "GAIN_LOSS_TYPE", length = 20)
    private String gainLossType;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "FF_REF", length = 100)
    private String ffRef;

    @Column(name = "FDA_REF")
    private Long fdaRef;

    @Column(name = "SALES_QTN_REF")
    private Long salesQtnRef;

    @Column(name = "MTA_REF", length = 20)
    private String mtaRef;

    @Column(name = "PAYING_TO_NAME", length = 100)
    private String payingToName;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "FILE_GENERATED", length = 1)
    private String fileGenerated;

    @Column(name = "FILE_NAME", length = 4000)
    private String fileName;

    @Column(name = "FILE_UNIQUE_ID")
    private Long fileUniqueId;

    @Column(name = "FILE_GENERATED_BY", length = 100)
    private String fileGeneratedBy;

    @Column(name = "FILE_GENERATED_DATE")
    private LocalDateTime fileGeneratedDate;

    @Column(name = "TT_CHARGE_TYPE", length = 20)
    private String ttChargeType;

    @Column(name = "TT_SPECIAL_RATE")
    private BigDecimal ttSpecialRate;

    @Column(name = "BENEFICIARY_IBAN", length = 25)
    private String beneficiaryIban;

    @Column(name = "BENEFICIARY_BANK_POID")
    private Long beneficiaryBankPoid;

    @Column(name = "SUPPRESS_VALIDATION", length = 1)
    private String suppressValidation;

    @Column(name = "RATE_DEAL_NO", length = 50)
    private String rateDealNo;

    @Column(name = "TAX_AMOUNT")
    private BigDecimal taxAmount;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Column(name = "TAX_PERCENTAGE")
    private BigDecimal taxPercentage;

    @Column(name = "BANK_PURPOSE_POID")
    private Long bankPurposePoid;

//    @Column(name = "CONFIDENTIAL_REMARKS", length = 1000)
//    private String confidentialRemarks;
}
