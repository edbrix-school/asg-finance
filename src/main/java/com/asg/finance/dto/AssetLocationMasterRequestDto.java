package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetLocationMasterRequestDto {


    @NotBlank(message = "Location Code is required")
    private String locationCode;

    @NotBlank(message = "Description is required")
    private String description;

    private Integer seqNo;

    private String active;
}