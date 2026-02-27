package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "AR_DEBIT_NOTE_HDR")
@Data
public class ArDebitNoteHdr {

    @Id
    @AuditIgnore
    @GeneratedValue(generator = "trigger-generated")
    @org.hibernate.annotations.GenericGenerator(
            name = "trigger-generated",
            strategy = "org.hibernate.id.IdentityGenerator"
    )
    @Column(name = "TRANSACTION_POID", updatable = false, insertable = false)
    private Long transactionPoid;

    @AuditIgnore
    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @AuditIgnore
    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @AuditIgnore
    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, nullable = false)
    private String docRef;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "PARTY_TYPE", length = 100)
    private String partyType;

    @Column(name = "PARTY_POID")
    private Long partyPoid;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "FDA_REF", length = 100)
    private String fdaRef;

    @AuditIgnore
    @Column(name = "FF_REF", length = 100)
    private String ffRef;

    @AuditIgnore
    @Column(name = "TDR_REF", length = 100)
    private String tdrRef;

    @AuditIgnore
    @Column(name = "ADDRESS_POID")
    private Long addressPoid;

    @Column(name = "DR_TOTAL")
    private BigDecimal drTotal;

    @Column(name = "CR_TOTAL")
    private BigDecimal crTotal;

    @Column(name = "POSTING_NARRATION", length = 500)
    private String postingNarration;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "GRAND_TOTAL")
    private BigDecimal grandTotal;

    @AuditIgnore
    @Column(name = "VOYAGE_REF", length = 100)
    private String voyageRef;

    @Column(name = "FDA_DIRECT_REF", length = 100)
    private String fdaDirectRef;

    @AuditIgnore
    @Column(name = "PROPERTY_INVOICE", length = 1)
    private String propertyInvoice = "N";

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Column(name = "CREDIT_PERIOD")
    private Integer creditPeriod;

    @AuditIgnore
    @Column(name = "PRINT_COMPANY_POID")
    private Long printCompanyPoid;

    @Column(name = "PO_REF", length = 300)
    private String poRef;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @AuditIgnore
    @Column(name = "USD_ONLY", length = 1)
    private String usdOnly = "N";

    @Column(name = "REMARKS_PRINTABLE", length = 1)
    private String remarksPrintable = "N";

    @Column(name = "TIN_NUMBER", length = 100)
    private String tinNumber;

    @Column(name = "OTHER_CURR_AMOUNT")
    private BigDecimal otherCurrAmount;

    @Column(name = "BHD_AMOUNT")
    private BigDecimal bhdAmount;

    @Column(name = "VOUCHER_TYPE", length = 100)
    private String voucherType;

    @Column(name = "COST_CENTER_POID")
    private Long costCenterPoid;

    @AuditIgnore
    @Column(name = "DISPOSAL_JV_POID")
    private Long disposalJvPoid;

    @AuditIgnore
    @Column(name = "COST_GROUP", length = 300)
    private String costGroup;

    @AuditIgnore
    @Column(name = "PARTY_REF", length = 2500)
    private String partyRef;

    @Column(name = "PRINT_DIVISION_POID")
    private Long printDivisionPoid = 1L;

    @Column(name = "SHOW_BANK_DETAILS_IN_PRINT", length = 1)
    private String showBankDetailsInPrint = "Y";

    @Column(name = "COST_REF_NUMBER", length = 500)
    private String costRefNumber;

    @AuditIgnore
    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany = "N";

    @AuditIgnore
    @Column(name = "TEMP_MEMNO", length = 21)
    private String tempMemno;

    @AuditIgnore
    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @AuditIgnore
    @Column(name = "DELETED", length = 1)
    private String deleted = "N";

    @OneToMany(mappedBy = "debitNoteHdr", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<ArDebitNoteDtl> glDetails;

    @OneToMany(mappedBy = "debitNoteHdr", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<ArDebitNoteChargeDtl> chargeDetails;
}