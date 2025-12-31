package com.asg.finance.dto.masters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAssetRequestDto {

    private Long groupPoid;


    @Size(max = 20, message = "FA Code must not exceed 20 characters")
    private String faCode;

    @NotBlank(message = "FA Description is mandatory")
    @Size(max = 100, message = "FA Description must not exceed 100 characters")
    private String faDescription;

    @Size(max = 100, message = "FA Description2 must not exceed 100 characters")
    private String faDescription2;

    @NotBlank(message = "Asset Type is mandatory")
    @Size(max = 300, message = "Asset Type must not exceed 300 characters")
    private String assetType;

    @NotNull(message = "FA Category POID is mandatory")
    private Long faCategoryPoid;

    @NotNull(message = "Location POID is mandatory")
    private Long locationPoid;

    @NotNull(message = "Company POID is mandatory")
    private Long companyPoid;

    @Size(max = 30, message = "Barcode must not exceed 30 characters")
    private String barcode;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @Size(max = 1, message = "Active must not exceed 1 character")
    private String active;

    private Integer seqNo;

    private String deleted;

    @Size(max = 1, message = "Mail Alert must not exceed 1 character")
    private String mailAlert;

    @Size(max = 1, message = "Verified must not exceed 1 character")
    private String verified;

    private LocalDateTime verifiedDate;

    private LocalDate assetValueDate;

    @Size(max = 100, message = "FA Type must not exceed 100 characters")
    private String faType;

    private Long faParentPoid;

    @Valid
    private GeneralInfoDto generalInfoDto;

    @Valid
    private InformationAssetDetailsDto informationAssetDetailsDto;

    @Valid
    private OpeningDetailsDto openingDetailsDto;

    @Valid
    private DepreciationDetailsDto depreciationDetailsDto;

    private VehicleDetailsDto vehicleDetailsDto;
}
