package com.asg.finance.dto.masters;

import com.asg.common.lib.dto.DetailsDto;
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
public class FixedAssetResponseDto {
    private Long faPoid;

    private Long groupPoid;
    private String faCode;
    private String faDescription;
    private String faDescription2;
    private String assetType;
    private Long faCategoryPoid;
    private Long locationPoid;
    private Long companyPoid;
    private String barcode;
    private String remarks;
    private String active;
    private Integer seqNo;
    private String deleted;
    private String mailAlert;
    private String verified;
    private LocalDateTime verifiedDate;
    private LocalDate assetValueDate;
    private String faType;
    private Long faParentPoid;
    private DetailsDto locationPoidDet;
    private DetailsDto faCategoryPoidDet;
    private DetailsDto companyPoidDet;


    private GeneralInfoDto generalInfoDto;
    private InformationAssetDetailsDto informationAssetDetailsDto;
    private OpeningDetailsDto openingDetailsDto;
    private DepreciationDetailsDto depreciationDetailsDto;
    private VehicleDetailsDto vehicleDetailsDto;
    private LocalDateTime createdDate;
    private String createdBy;

}
