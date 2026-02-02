package com.asg.finance.entity.master;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GLOBAL_INSURANCE_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceMaster {
    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "insurance_seq")
    @SequenceGenerator(name = "insurance_seq", sequenceName = "GLOBAL_INSURANCE_MASTER_SEQ", allocationSize = 1)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Column(name = "GROUP_POID", nullable = false)
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID", nullable = false)
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "TRANSACTION_DATE")
    @AuditIgnore
    private LocalDate transactionDate;

    @Column(name = "DOC_REF", length = 25)
    private String docRef;

    @Pattern(regexp = "^(VEHICLE_INSURANCE|MEDICAL_INSURANCE|PROPERTY_INSURANCE|TRAVEL_INSURANCE|PROJECTS_INSURANCE|RO_RO_INSURANCE|EQUIPMENT_INSURANCE|CUSTOMS_CLEARANCE_INSURANCE|IT_INSURANCE|LIFE_INSURANCE|CYBER_SECURITY_INSURANCE|PROFESSIONAL_INDEMNITY_INSURANCE|FORWARDING_LOGISTICS_INSURANCE)$",
            message = "Invalid insurance type")
    @Column(name = "INSURANCE_TYPE", length = 50)
    private String insuranceType;

    @Pattern(regexp = "^(GROUP|COMPANY|INDIVIDUAL)$",
            message = "Invalid insurance category")
    @Column(name = "INSURANCE_CATEGORY", length = 50)
    private String insuranceCategory;

    @Pattern(regexp = "^POL[0-9]{8}$",
            message = "Policy number must be in format POL12345678")
    @Column(name = "POLICY_NO", length = 50)
    private String policyNo;

    @Column(name = "INSURANCE_PROVIDER", length = 200)
    private String insuranceProvider;

    @Column(name = "FROM_DATE")
    private LocalDate fromDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDate expiryDate;

    @Column(name = "CURRENCYPOID")
    private Long currencyPoid;

    @Column(name = "EXCHANGE_RATE")
    @AuditIgnore
    private BigDecimal exchangeRate;

    @Column(name = "INSURANCE_AMOUNT")
    private BigDecimal insuranceAmount;

    @Column(name = "PREMIUM_AMOUNT")
    private BigDecimal premiumAmount;

    @Column(name = "PAYMENT_FREQUENCY", length = 100)
    private String paymentFrequency;

    @Column(name = "ONE_TIME", length = 1)
    private String oneTime;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "PJ_REF_POID")
    @AuditIgnore
    private Long pjRefPoid;

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

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsuranceVehicleDetail> vehicleDetails;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsuranceEmployeeDetail> employeeDetails;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsurancePropertyDetail> propertyDetails;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsuranceDetail> insuranceDetails;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsurancePicDetail> picDetails;

    @OneToMany(mappedBy = "insuranceMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @AuditIgnore
    private List<InsuranceRenewalLog> renewalLogs;
}