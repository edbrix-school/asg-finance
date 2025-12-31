package com.asg.finance.entity;

import com.asg.finance.entity.key.GlBankFileDtlKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_BANK_FILE_DTL")
@IdClass(GlBankFileDtlKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankFileDtl {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "DEBIT_TRANSACTION_POID")
    private Long debitTransactionPoid;

    @Column(name = "DEBIT_TRANSACTION_DATE")
    private LocalDate debitTransactionDate;

    @Column(name = "DEBIT_COMPANY_POID")
    private Long debitCompanyPoid;

    @Column(name = "DEBIT_DOC_REF", length = 100)
    private String debitDocRef;

    @Column(name = "DEBIT_PAYING_TO_NAME", length = 200)
    private String debitPayingToName;

    @Column(name = "DEBIT_PAYING_TYPE", length = 10)
    private String debitPayingType;

    @Column(name = "DEBIT_LONG_NARRATION", length = 500)
    private String debitLongNarration;

    @Column(name = "DEBIT_TT_DATE")
    private LocalDate debitTtDate;

    @Column(name = "DEBIT_CURRENCY_CODE", length = 10)
    private String debitCurrencyCode;

    @Column(name = "DEBIT_CURRENCY_RATE", precision = 18, scale = 6)
    private BigDecimal debitCurrencyRate;

    @Column(name = "DEBIT_CURRENCY_AMT", precision = 18, scale = 2)
    private BigDecimal debitCurrencyAmt;

    @Column(name = "DEBIT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "DRILLDOWN_LINK_INFO", length = 500)
    private String drilldownLinkInfo;

    @Column(name = "DEBIT_TT_CHARGE_TYPE", length = 50)
    private String debitTtChargeType;

    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private java.time.LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 50)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private java.time.LocalDateTime lastModifiedDate;

    @Column(name = "SELECTED", length = 1)
    private String selected;
    
//    @Column(name = "ONLY_APPROVAL", length = 1)
//    private String onlyApproval;
    
//    @Column(name = "TT_SUPPRESS_BALANCE_CHECK", length = 1)
//    private String ttSuppressBalanceCheck;
}