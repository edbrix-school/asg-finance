package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Timestamp;

@Entity
@Table(name = "AP_PURCHASE_CN_HDR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApPurchaseCnHdr {

    @Id
    @GeneratedValue(generator = "trigger-generated")
    @org.hibernate.annotations.GenericGenerator(
            name = "trigger-generated",
            strategy = "org.hibernate.id.IdentityGenerator"
    )
    @Column(name = "TRANSACTION_POID", updatable = false, insertable = false)
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "DOC_REF", length = 25, nullable = false)
    private String docRef;

    @Column(name = "PO_REF", length = 100)
    private String poRef;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "CURRENCY_CODE", length = 20)
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private BigDecimal currencyRate;

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "SUB_TOTAL")
    private BigDecimal subTotal;

    @Column(name = "DISCOUNT")
    private BigDecimal discount;

    @Column(name = "GRAND_TOTAL")
    private BigDecimal grandTotal;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "ITEM_TOTAL")
    private BigDecimal itemTotal;

    @Column(name = "CHARGE_TOTAL")
    private BigDecimal chargeTotal;

    @Column(name = "GL_TOTAL")
    private BigDecimal glTotal;

    @Column(name = "TYPE", length = 20)
    private String type;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CREDIT_PERIOD")
    private Long creditPeriod;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;

    @Column(name = "NARRATION", length = 500)
    private String narration;

    @Column(name = "SUPPLIER_CN_DATE")
    private LocalDate supplierCnDate;

    @Column(name = "SUPPLIER_CN_NO", length = 100)
    private String supplierCnNo;

    @Column(name = "SUPPLIER_CN_REMARK", length = 500)
    private String supplierCnRemark;

    @Column(name = "MULTI_COMPANY", length = 1)
    private String multiCompany = "N";

    @Column(name = "BHD_AMOUNT")
    private BigDecimal bhdAmount;

    @Column(name = "SUPPLIER_CN_AMOUNT")
    private BigDecimal supplierCnAmount;

    @Column(name = "ROUNDING_AMOUNT")
    private BigDecimal roundingAmount;

    @Column(name = "BILL_TYPE", length = 30)
    private String billType;

    @Column(name = "PARTY_TYPE", length = 100)
    private String partyType;

    @Column(name = "PARTY_TIN_NUMBER", length = 100)
    private String partyTinNumber;

    @Column(name = "FDA_COVERING_REF", length = 100)
    private String fdaCoveringRef;

    @Column(name = "PJ_REVERSAL_REF")
    private Long pjReversalRef;

    @Column(name = "PJ_REVERSAL_REF_TYPE", length = 100)
    private String pjReversalRefType;

    @Column(name = "PJ_REVERSAL_REF_DETAILS", length = 500)
    private String pjReversalRefDetails;

    @Column(name = "FF_REF")
    private Long ffRef;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private Timestamp lastModifiedDate;

    @Column(name = "DELETED", length = 1)
    private String deleted = "N";
}
