package com.asg.finance.dto.masters;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationAssetDetailsDto {

    @NotBlank(message = "Type of Information Asset is mandatory")
    @Size(max = 300, message = "Type of Information Asset must not exceed 300 characters")
    private String typeOfInformationAsset;

    @Size(max = 1, message = "Personal Data must not exceed 1 character")
    private String personalData;

    @Size(max = 1, message = "Personal Sensitive Data must not exceed 1 character")
    private String personalSensitiveData;

    @Size(max = 1, message = "Sensitive Customer Data must not exceed 1 character")
    private String sensitiveCustomerData;

    @NotBlank(message = "Asset Classification is mandatory")
    @Size(max = 300, message = "Asset Classification must not exceed 300 characters")
    private String assetClassification;

    @NotBlank(message = "Integrity is mandatory")
    @Size(max = 100, message = "Integrity must not exceed 100 characters")
    private String integrity;

    @NotBlank(message = "Availability is mandatory")
    @Size(max = 100, message = "Availability must not exceed 100 characters")
    private String availability;

    @NotBlank(message = "Data Retention Period is mandatory")
    @Size(max = 300, message = "Data Retention Period must not exceed 300 characters")
    private String dataRetentionPeriod;
}
