package com.asg.finance.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Information Asset Master operations
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInformationMasterResponse {

    private Long iaPoid;
    private Long groupPoid;
    private Long companyPoid;

    private String iaCode;
    private String iaName;
    private String iaDescription;
    private String operatingUnit;

    private String typeOfInformationAsset;
    private String assetCustodian;
    private String assetClassification;

    private String integrity;
    private String availability;
    private String dataRetentionPeriod;

    private String personalData;
    private String personalSensitiveData;
    private String sensitiveCustomerData;

    private String active;
    private String deleted;
    private Integer seqNo;

    private String processName;
    private String processOwner;

    private String atOrigin;
    private String informationisMoved;

    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

