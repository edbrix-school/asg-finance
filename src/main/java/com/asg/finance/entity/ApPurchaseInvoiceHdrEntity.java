package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AP_PURCHASE_INVOICE_HDR")
public class ApPurchaseInvoiceHdrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "TRANSACTION_DATE")
    private LocalDate transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "DOC_REF", insertable = false, updatable = false)
    private String docRef;

    @Column(name = "PO_REF")
    private String poRef;

    @Column(name = "FDA_REF")
    private String fdaRef;

    @Column(name = "FF_REF")
    private String ffRef;

    @Column(name = "SHIP_REF")
    private String shipRef;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "CURRENCY_CODE")
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private Long currencyRate;

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "LOCATION_POID")
    private Long locationPoid;

    @Column(name = "SUB_TOTAL")
    private Long subTotal;

    @Column(name = "DISCOUNT")
    private Long discount;

    @Column(name = "EXPENSE_BY_SUPPLIER")
    private Long expenseBySupplier;

    @Column(name = "GRAND_TOTAL")
    private Long grandTotal;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate; // TIMESTAMP

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate; // TIMESTAMP

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "ITEM_TOTAL")
    private Long itemTotal;

    @Column(name = "CHARGE_TOTAL")
    private Long chargeTotal;

    @Column(name = "GL_TOTAL")
    private Long glTotal;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CREDIT_PERIOD")
    private Long creditPeriod;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate; // DATE

    @Column(name = "INVNO_OLD")
    private String invnoOld;

    @Column(name = "MODCODE_OLD")
    private String modcodeOld;

    @Column(name = "REF_TYPE")
    private String refType;

    @Column(name = "SALES_QTN_POID")
    private String salesQtnPoid;

    @Column(name = "NARRATION")
    private String narration;

    @Column(name = "SUPPLIER_INV_DATE")
    private LocalDate supplierInvDate; // DATE

    @Column(name = "SUPPLIER_INV_NO")
    private String supplierInvNo;

    @Column(name = "SUPPLIER_INV_REMARK")
    private String supplierInvRemark;

    @Column(name = "MTA_REF")
    private String mtaRef;

    @Column(name = "MULTI_COMPANY")
    private String multiCompany;

    @Column(name = "BHD_AMOUNT")
    private Long bhdAmount;

    @Column(name = "SUPPLIER_INV_AMOUNT")
    private Long supplierInvAmount;

    @Column(name = "ROUNDING_AMOUNT")
    private Long roundingAmount;

    @Column(name = "BILL_TYPE")
    private String billType;

    @Column(name = "PROVISIONAL_INVOICE")
    private String provisionalInvoice;

    @Column(name = "PARTY_TYPE")
    private String partyType;

    @Column(name = "GRN_SUPPLIER_POID")
    private Long grnSupplierPoid;

    @Column(name = "PARTY_TIN_NUMBER")
    private String partyTinNumber;

    @Column(name = "PAID_AGAINST")
    private String paidAgainst;

    @Column(name = "FDA_COVERING_REF")
    private String fdaCoveringRef;
}
