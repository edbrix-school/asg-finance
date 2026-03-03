package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "INFORMATION_ASSET_MASTER")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInformationMasterEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IA_POID")
    @AuditIgnore
    private Long iaPoid;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "IA_CODE")
    @AuditIgnore
    private String iaCode;

    @Column(name = "IA_NAME")
    private String iaName;

    @Column(name = "IA_DESCRIPTION")
    private String iaDescription;

    @Column(name = "OPERATING_UNIT")
    private String operatingUnit;

    @Column(name = "TYPE_OF_INFORMATION_ASSET")
    private String typeOfInformationAsset;

    @Column(name = "ASSET_CUSTODIAN")
    private String assetCustodian;

    @Column(name = "ASSET_CLASSIFICATION")
    private String assetClassification;

    @Column(name = "INTEGRITY")
    private String integrity;

    @Column(name = "AVAILABILITY")
    private String availability;

    @Column(name = "DATA_RETENTION_PERIOD")
    private String dataRetentionPeriod;

    @Column(name = "PERSONAL_DATA")
    private String personalData;

    @Column(name = "PERSONAL_SENSITIVE_DATA")
    private String personalSensitiveData;

    @Column(name = "SENSITIVE_CUSTOMER_DATA")
    private String sensitiveCustomerData;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "DELETED")
    @AuditIgnore
    private String deleted;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "PROCESS_NAME")
    private String processName;

    @Column(name = "PROCESS_OWNER")
    private String processOwner;

    @Column(name = "IA_PROTECTION_LEAVEL_ORIGIN")
    private String protectionLevelOrigin;

    @Column(name = "IA_PROTECTION_LEAVEL_MOVED")
    private String protectionLevelMoved;
}

