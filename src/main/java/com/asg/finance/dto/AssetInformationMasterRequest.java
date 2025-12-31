package com.asg.finance.dto;

import com.asg.finance.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Information Asset Master operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetInformationMasterRequest {

    @NotBlank(message = "IA Code is required", groups = OnUpdate.class)
    @Size(max = 20, message = "IA Code must not exceed 20 characters")
    private String iaCode;

    @Size(min = 1, max = 100, message = "IA Name must not exceed 100 characters")
    private String iaName;

    @Size(max = 1000, message = "IA Description must not exceed 1000 characters")
    private String iaDescription;

    @Size(max = 300, message = "Operating Unit must not exceed 300 characters")
    private String operatingUnit;

    @Size(max = 300, message = "Type of Information Asset must not exceed 300 characters")
    private String typeOfInformationAsset;

    @Builder.Default
    @Size(max = 1, message = "Personal Data must not exceed 1 character")
    private String personalData = "N";

    @Builder.Default
    @Size(max = 1, message = "Personal Sensitive Data must not exceed 1 character")
    private String personalSensitiveData = "N";

    @Builder.Default
    @Size(max = 1, message = "Sensitive Customer Data must not exceed 1 character")
    private String sensitiveCustomerData = "N";

    @Size(max = 300, message = "Asset Classification must not exceed 300 characters")
    private String assetClassification;

    @Size(max = 100, message = "Integrity must not exceed 100 characters")
    private String integrity;

    @Size(max = 100, message = "Availability must not exceed 100 characters")
    private String availability;

    @Size(max = 300, message = "Data Retention Period must not exceed 300 characters")
    private String dataRetentionPeriod;

    @Size(max = 300, message = "Asset Custodian must not exceed 300 characters")
    private String assetCustodian;

    @Size(max = 2000, message = "Protection Level at Origin must not exceed 2000 characters")
    private String atOrigin;

    @Size(max = 2000, message = "Protection Level Moved must not exceed 2000 characters")
    private String informationisMoved;

    @Size(max = 300, message = "Process Name must not exceed 300 characters")
    private String processName;

    @Size(max = 300, message = "Process Owner must not exceed 300 characters")
    private String processOwner;

    private Integer seqNo;

    @Builder.Default
    @Size(max = 1, message = "Active must not exceed 1 character")
    private String active = "Y";
}
