package com.asg.finance.entity.master;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "FIXED_ASSET_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FA_POID", nullable = false)
    @AuditIgnore
    private Long faPoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "FA_CODE", length = 20, unique = true)
    @AuditIgnore
    private String faCode;

    @Column(name = "FA_DESCRIPTION", length = 100)
    private String faDescription;

    @Column(name = "FA_DESCRIPTION2", length = 100)
    private String faDescription2;

    @Column(name = "ASSET_TYPE", length = 300)
    private String assetType;

    @Column(name = "FA_CATEGORY_POID")
    private Long faCategoryPoid;

    @Column(name = "LOCATION_POID")
    private Long locationPoid;

    @Column(name = "BARCODE", length = 30)
    private String barcode;

    @Lob
    @Column(name = "FA_PHOTO")
    @AuditIgnore
    private byte[] faPhoto;

    @Column(name = "FA_OWNER", length = 100)
    private String faOwner;

    @Column(name = "HANDLED_BY", length = 100)
    @AuditIgnore
    private String handledBy;

    @Column(name = "MODEL_NO", length = 100)
    private String modelNo;

    @Column(name = "SERIAL_NO", length = 100)
    private String serialNo;

    @Column(name = "FA_COLOR", length = 100)
    private String faColor;

    @Column(name = "FA_SIZE", length = 100)
    private String faSize;

    @Column(name = "COUNTRY_OF_ORGIN", length = 100)
    private String countryOfOrigin;

    @Column(name = "SUPPLIER_POID")
    private Long supplierPoid;

    @Column(name = "PURCHASE_DATE")
    private LocalDate purchaseDate;

    @Column(name = "INVOICE_NO", length = 100)
    private String invoiceNo;

    @Column(name = "WARRANTY_PERIOD", length = 100)
    private String warrantyPeriod;

    @Column(name = "MAINTENACE_CONTRACT", length = 300)
    private String maintenanceContract;

    @Column(name = "AMC_SUPPLIER", length = 100)
    private String amcSupplier;

    @Column(name = "CONTRACT_EXPIRY")
    private LocalDate contractExpiry;

    @Column(name = "GROSS_VALUE")
    private BigDecimal grossValue;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "VEH_PRODUCTION_YEAR")
    private Integer vehicleProductionYear;

    @Column(name = "REGISTRATION_NO")
    private Long registrationNo;

    @Column(name = "BRAND", length = 100)
    private String brand;

    @Column(name = "MAKE_DATE")
    private LocalDate makeDate;

    @Column(name = "ENGINE_CHASIS_NO", length = 100)
    private String engineChassisNo;

    @Column(name = "VEHICLE_TYPE", length = 100)
    private String vehicleType;

    @Column(name = "INSURANCE_COVERAGE", length = 100)
    private String insuranceCoverage;

    @Column(name = "INSURANCE_RENEWAL_DATE")
    private LocalDate insuranceRenewalDate;

    @Column(name = "INSURANCE_COMPANY", length = 100)
    private String insuranceCompany;

    @Column(name = "INSURANCE_POLICY_NO", length = 100)
    private String insurancePolicyNo;

    @Column(name = "INSURANCE_AMOUNT")
    private BigDecimal insuranceAmount;

    @Column(name = "DEPRECIABLE", length = 1)
    private String depreciable;

    @Column(name = "DEPRECIATION_METHOD", length = 20)
    private String depreciationMethod;

    @Column(name = "DEPRECIATION_START_DATE")
    private LocalDate depreciationStartDate;

    @Column(name = "SCRAP_VALUE")
    private BigDecimal scrapValue;

    @Column(name = "DEPRECIATION_PERCENT")
    private BigDecimal depreciationPercent;

    @Column(name = "ASSET_LIFE", length = 20)
    private String assetLife;

    @Column(name = "OPENING_ASSET", length = 1)
    private String openingAsset;

    @Column(name = "OPENING_ASSET_VALUE")
    private BigDecimal openingAssetValue;

    @Column(name = "ACC_DEPRECIATED_AMT")
    private BigDecimal accumulatedDepreciation;

    @Column(name = "WDV_VALUE")
    private BigDecimal wdValue;

    @Column(name = "FA_GL_ACCOUNT")
    private Long faGlAccount;

    @Column(name = "FA_ACCUMULATION_AC")
    private Long faAccumulationAccount;

    @Column(name = "FA_DEPRECIATION_AC")
    private Long faDepreciationAccount;

    @Column(name = "COST_CENTER", length = 20)
    private String costCenter;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO", precision = 5)
    private Integer seqNo;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "SCRAP_DATE")
    private LocalDate scrapDate;

    @Column(name = "SYSTEM_SPECIFICATIONS", length = 3000)
    private String systemSpecifications;

    @Column(name = "SOFTWARE_DETAILS", length = 3000)
    private String softwareDetails;

    @Column(name = "EMPLOYEE_POID")
    private Long employeePoid;

    @Column(name = "MAIL_ALERT", length = 1)
    private String mailAlert;

    @AuditIgnore
    @Column(name = "BATCH_CREATION_REF", length = 100)
    private String batchCreationRef;

    @Column(name = "VERIFIED", length = 1)
    private String verified;

    @Column(name = "VERIFIED_DATE")
    private LocalDateTime verifiedDate;

    @Column(name = "ASSET_VALUE_DATE")
    private LocalDate assetValueDate;

    @Column(name = "FA_PARENT_POID")
    private Long faParentPoid;

    @Column(name = "FA_TYPE", length = 100)
    private String faType;

    @Column(name = "TYPE_OF_INFORMATION_ASSET", length = 300)
    private String typeOfInformationAsset;

    @Column(name = "PERSONAL_DATA", length = 1)
    private String personalData;

    @Column(name = "PERSONAL_SENSITIVE_DATA", length = 1)
    private String personalSensitiveData;

    @Column(name = "SENSITIVE_CUSTOMER_DATA", length = 1)
    private String sensitiveCustomerData;

    @Column(name = "ASSET_CLASSIFICATION", length = 300)
    private String assetClassification;

    @Column(name = "INTEGRITY", length = 100)
    private String integrity;

    @Column(name = "AVAILABILITY", length = 100)
    private String availability;

    @Column(name = "DATA_RETENTION_PERIOD", length = 300)
    private String dataRetentionPeriod;
}
