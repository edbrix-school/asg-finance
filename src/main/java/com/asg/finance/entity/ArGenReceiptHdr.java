package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AR_GEN_RECEIPT_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArGenReceiptHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", insertable = false, updatable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 30, unique = true)
    private String docRef;

    @Column(name = "RCVD_FROM_POID")
    private Long rcvdFromPoid;

    @Column(name = "RCPT_AMOUNT")
    private BigDecimal rcptAmount;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "BL_POID")
    private Long blPoid;

    @Column(name = "BL_RELEASE_TYPE", length = 20)
    private String blReleaseType;

    @Column(name = "DATA_LOADED", length = 1)
    private String dataLoaded;

    @Column(name = "RCVD_OTH_POID")
    private Long rcvdOthPoid;

    @Column(name = "RCVD_TYPE", length = 20)
    private String rcvdType;

    @Column(name = "GL_BALANCE")
    private BigDecimal glBalance;

    @Column(name = "RCVD_FROM_DTL_PRINT", length = 200)
    private String rcvdFromDtlPrint;

    @Column(name = "REF_NUMBER", length = 20)
    private String refNumber;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;

    @Column(name = "VERIFIED", length = 1)
    private String verified;

    @Column(name = "MULTICOMPANY", length = 1)
    private String multicompany;

    @Column(name = "PRINT_DOC_COMP_ID")
    private Long printDocCompId;

    @Column(name = "BANK_CHARGES")
    private BigDecimal bankCharges;

    @Column(name = "EX_GAIN_LOSS")
    private BigDecimal exGainLoss;

    @Column(name = "TT_BANK_POID")
    private Long ttBankPoid;

    @Column(name = "ROUND_OFF")
    private BigDecimal roundOff;

    @Column(name = "COST_CENTER_POID", length = 50)
    private String costCenterPoid;

    @Column(name = "LINE_TYPE", length = 100)
    private String lineType;

    @Column(name = "EXTRA_CHARGES", length = 1)
    private String extraCharges;

    @Column(name = "BILL_AMOUNT")
    private BigDecimal billAmount;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "INVOICE_AMOUNT")
    private BigDecimal invoiceAmount;

    @Column(name = "CLIENT_POID")
    private Long clientPoid;

    @Column(name = "CLIENT_DEPOSIT_REQUEST_POID")
    private Long clientDepositRequestPoid;

    @OneToMany(mappedBy = "receiptHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ArGenReceiptPymtDetails> paymentDetails = new ArrayList<>();

    @OneToMany(mappedBy = "receiptHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ArGenReceiptBillDtl> billDetails = new ArrayList<>();

    @OneToMany(mappedBy = "receiptHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ArGenReceiptChargesDtl> chargesDetails = new ArrayList<>();

    @OneToMany(mappedBy = "receiptHdr", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ArGenReceiptAdvanceDtl> advanceDetails = new ArrayList<>();

    // Helper methods to manage bidirectional relationships
    public void addPaymentDetail(ArGenReceiptPymtDetails detail) {
        paymentDetails.add(detail);
        detail.setReceiptHdr(this);
    }

    public void addBillDetail(ArGenReceiptBillDtl detail) {
        billDetails.add(detail);
        detail.setReceiptHdr(this);
    }

    public void addChargeDetail(ArGenReceiptChargesDtl detail) {
        chargesDetails.add(detail);
        detail.setReceiptHdr(this);
    }

    public void addAdvanceDetail(ArGenReceiptAdvanceDtl detail) {
        advanceDetails.add(detail);
        detail.setReceiptHdr(this);
    }
}

