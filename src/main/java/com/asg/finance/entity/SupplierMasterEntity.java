package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "AP_SUPPLIER_MASTER")
@Data
public class SupplierMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "SUPPLIER_CODE")
    private String supplierCode;

    @Column(name = "SUPPLIER_NAME")
    private String supplierName;

    @Column(name = "SUPPLIER_NAME2")
    private String supplierName2;

    @Column(name = "SUPPLIER_TYPE")
    private String supplierType;

    @Column(name = "SUPPLIER_CATEGORY_POID")
    private Long supplierCategoryPoid;

    @Column(name = "COUNTRY_POID")
    private Long countryPoid;

    @Column(name = "CREDIT_LIMIT")
    private Long creditLimit;

    @Column(name = "CREDIT_PERIOD")
    private Long creditPeriod;

    @Column(name = "CR_NO")
    private String crNo;

    @Column(name = "CONTACT_PERSON")
    private String contactPerson;

    @Column(name = "ADDRESS_POID")
    private Long addressPoid;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDate createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDate lastModifiedDate;

    @Column(name = "GENERAL_REMARKS")
    private String generalRemarks;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "TEMP_PAYMENT_NAME")
    private String tempPaymentName;

    @Column(name = "CURRENCY_CODE")
    private String currencyCode;

    @Column(name = "CURRENCY_RATE")
    private Long currencyRate;

    @Column(name = "RATE_EXPIRY_DATE")
    private LocalDate rateExpiryDate;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "DEFAULT_WEIGHT_SELECTION_MTD")
    private String defaultWeightSelectionMethod;

    @Column(name = "PRODUCT_INFO")
    private String productInfo;

    @Column(name = "TIN_NUMBER")
    private String tinNumber;

    @Column(name = "TAX_SLAB")
    private String taxSlab;

    @Column(name = "EXEMPTION_REASON")
    private String exemptionReason;

    @Column(name = "TAX_REGISTERED_DATE")
    private LocalDate taxRegisteredDate;

    @Column(name = "PURCHASER")
    private Long purchaserPoid;

    @Column(name = "GRN_CREDIT_GL")
    private Long grnCreditGl;

    @Column(name = "AUDITED_YEAR")
    private LocalDate auditedYear;

    @Column(name = "AUDITING_FIRM")
    private String auditingFirm;

    @Column(name = "ISO_CERTIFICATION")
    private String isoCertification;

    @Column(name = "PROFILE_UPDATED")
    private String profileUpdated;

    @Column(name = "PROFILE_VAT_CR_MISMATCH")
    private String profileVatCrMismatch;

    @Column(name = "CUSTOMER_POID")
    private Long customerPoid;
}
