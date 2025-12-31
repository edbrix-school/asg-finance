package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_BANK_PAYMENT_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GLPaymentVoucherHDREntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, nullable = false)
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

    @Column(name = "CHQ_CARDNO", length = 50)
    private String chqCardNo;

    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private Long currencyRate;

    @Column(name = "CURRENCY_AMOUNT")
    private Long currencyAmount;

    @Column(name = "LOCAL_AMOUNT")
    private Long localAmount;

    @Column(name = "SHORT_NARRATION", length = 200)
    private String shortNarration;

    @Column(name = "LONG_NARRATION", length = 2000)
    private String longNarration;

    @Column(name = "CHQ_PRINTED", length = 1)
    private String chqPrinted;

    @Column(name = "CHQ_PRINTED_USER_CODE", length = 25)
    private String chqPrintedUserCode;

    @Column(name = "CHQ_PRINTED_DATE")
    private LocalDate chqPrintedDate;

    @Column(name = "CHEQUE_ISSUE_PHYSICAL", length = 1)
    private String chequeIssuePhysical = "N";

    @Column(name = "RELEASED_DATE")
    private LocalDate releasedDate;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "PDC_BATCH_POID")
    private Long pdcBatchPoid;

    @Column(name = "AVAILABLE_BALANCE")
    private Long availableBalance;

    @Column(name = "BANK_BALANCE")
    private Long bankBalance;

    @Column(name = "PRE_PRINTED", length = 1)
    private String prePrinted = "N";

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany;

    @Column(name = "RELEASED", length = 1)
    private String released = "N";

    @Column(name = "RELEASED_BY_USER_CODE", length = 25)
    private String releasedByUserCode;

    @Column(name = "RELEASED_PERSON_ADDRESS", length = 100)
    private String releasedPersonAddress;

    @Column(name = "RELEASED_PERSON_ID", length = 100)
    private String releasedPersonId;

    @Column(name = "RELEASED_SEQNO")
    private Long releasedSeqNo;

    @Column(name = "RELEASED_TO_PERSON", length = 100)
    private String releasedToPerson;

    @Column(name = "FDA_REF")
    private Long fdaRef;

    @Column(name = "FF_REF", length = 100)
    private String ffRef;

    @Column(name = "MTA_REF", length = 20)
    private String mtaRef;

    @Column(name = "PO_REF", length = 20)
    private String poRef;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "SALES_QTN_REF")
    private Long salesQtnRef;

    @Column(name = "CHQ_SIGN_TYPE", length = 20)
    private String chqSignType;

    @Column(name = "OLD_PV_NAME", length = 150)
    private String oldPvName;

    @Column(name = "PAY_TO_OLDCODE", length = 20)
    private String payToOldCode;

    @Column(name = "ACCOUNT_PAYEE", length = 1)
    private String accountPayee;

    @Column(name = "RECONCILED_DATE")
    private LocalDate reconciledDate;

    @Column(name = "PRINT_WITHOUT_BILLWISE", length = 1)
    private String printWithoutBillwise = "N";

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "HOLD", length = 1)
    private String hold = "N";

    @Column(name = "SUPPRESS_VALIDATION", length = 1)
    private String suppressValidation;

    @Column(name = "SECURITY_CHEQUE", length = 1)
    private String securityCheque;

}
