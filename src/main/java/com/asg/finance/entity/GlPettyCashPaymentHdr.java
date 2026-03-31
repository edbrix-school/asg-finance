package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_PETTY_CASH_PAYMENT_HDR")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlPettyCashPaymentHdr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "DOC_REF", nullable = false, length = 25)
    private String docRef;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "PETTY_CASH_GL_POID", nullable = false)
    private Long pettyCashGlPoid;

    @Column(name = "BALANCE")
    private BigDecimal balance;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PAYING_TO", length = 100)
    private String payingTo;

    @Column(name = "NARRATION", length = 500)
    private String narration;

    @Column(name = "ADVANCE", length = 1)
    private String advance;

    @Column(name = "REF_TYPE", length = 100)
    private String refType;

    @Column(name = "FDA_REF", length = 100)
    private String fdaRef;

    @Column(name = "FF_REF", length = 100)
    private String ffRef;

    @Column(name = "SETTLED_DATE")
    private LocalDate settledDate;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "SETTLED_TOTAL")
    private BigDecimal settledTotal;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "GRAND_TOTAL")
    private BigDecimal grandTotal;

    @Column(name = "MTA_REF", length = 20)
    private String mtaRef;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany;

    @Column(name = "PO_REF", length = 20)
    private String poRef;

    @Column(name = "SALES_QTN_REF", length = 20)
    private String salesQtnRef;

    @Column(name = "CR_TOTAL")
    private BigDecimal crTotal;

    @Column(name = "DR_TOTAL")
    private BigDecimal drTotal;

    @Column(name = "ROUNDING_AMOUNT")
    private BigDecimal roundingAmount;

    @Column(name = "GRN_SUPPLIER_POID")
    private Long grnSupplierPoid;

    @Column(name = "SUPPLIER_GL_POID")
    private Long supplierGlPoid;

    @Column(name = "CUSTOMER_GL_POID")
    private Long customerGlPoid;

    @Column(name = "ADVANCE_PETTY_CASH_POID")
    private Long advancePettyCashPoid;

    @Column(name = "ADVANCE_STATUS", length = 100)
    private String advanceStatus;

    @Column(name = "ADVANCE_AMOUNT")
    private BigDecimal advanceAmount;

    @Column(name = "COMPANY_DIV_POID", columnDefinition = "NUMBER DEFAULT 1")
    private Long companyDivPoid;


}