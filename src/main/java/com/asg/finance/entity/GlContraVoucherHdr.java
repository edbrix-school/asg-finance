package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_CONTRA_VOUCHER_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlContraVoucherHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID", nullable = false)
    private Long transactionPoid;

    @Column(name = "CREDIT_GL", nullable = false)
    private Long creditGl;

    @Column(name = "DEBIT_GL", nullable = false)
    private Long debitGl;

    @Column(name = "CURRENCY_CODE", length = 3)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE", precision = 18, scale = 10)
    private BigDecimal currencyRate;

    @Column(name = "AMOUNT", precision = 18, scale = 3)
    private BigDecimal amount;

    @Column(name = "BHD_AMOUNT", precision = 18, scale = 3)
    private BigDecimal bhdAmount;

    @Column(name = "POSTING_NARRATION", length = 500)
    private String postingNarration;

    @Column(name = "CHEQUE_NO", length = 50)
    private String chequeNo;

    @Column(name = "MANUAL", length = 1)
    private String manual;

    @Column(name = "CHEQUE_DATE")
    private LocalDate chequeDate;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany;

    @Column(name = "DOC_REF", length = 25, nullable = true)
    private String docRef;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "OLD_JVNO", length = 30)
    private String oldJvno;

    @Column(name = "DR_TOTAL")
    private BigDecimal drTotal;

    @Column(name = "CR_TOTAL")
    private BigDecimal crTotal;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

}

