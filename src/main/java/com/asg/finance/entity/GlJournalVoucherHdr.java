package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_JOURNAL_VOUCHER_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlJournalVoucherHdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", length = 25, nullable = false, insertable = false, updatable = false)
    @Generated
    private String docRef;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "DR_TOTAL")
    private BigDecimal drTotal;

    @Column(name = "CR_TOTAL")
    private BigDecimal crTotal;

    @Column(name = "POSTING_NARRATION", length = 500)
    private String postingNarration;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

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

    @Column(name = "FA_DEPR_POST_DATE", length = 20)
    private String faDeprPostDate;

    @Column(name = "RECURRING_JV_POID")
    private Long recurringJvPoid;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany;

    @Column(name = "FA_POID")
    private Long faPoid;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "WDV_ACCOUNT_GL")
    private Long wdvAccountGl;

    @Column(name = "EXPENSE_ALLOCATION_POID")
    private Long expenseAllocationPoid;

    @Column(name = "POSTED_FROM_DOC_ID", length = 20)
    private String postedFromDocId;

    @Column(name = "POSTED_FROM_DOC_KEY_POID")
    private Long postedFromDocKeyPoid;

    @Column(name = "POSTED_FROM_DOC_REF", length = 60)
    private String postedFromDocRef;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "BHD_AMOUNT")
    private BigDecimal bhdAmount;

    @Column(name = "FA_DISPOSAL_JV_RPT_REF")
    private LocalDateTime faDisposalJvRptRef;

    @Column(name = "CONFIDENTIAL_REMARKS", length = 500)
    private String confidentialRemarks;
}
