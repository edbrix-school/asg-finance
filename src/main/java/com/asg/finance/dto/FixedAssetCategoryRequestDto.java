package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAssetCategoryRequestDto {

    @NotBlank(message = "FA Category Description is required")
    @Size(max = 300, message = "FA Category Code must be at most 300 characters")
    private String faCategoryDescription;

    @Size(max = 300, message = "FA Category Code must be at most 300 characters")
    private String faCategoryDescription2;

    @NotBlank(message = "Asset Type is required")
    @Size(max = 300, message = "Asset Type must be at most 300 characters")
    private String assetType;

    @NotNull(message = "GL Account is required")
    private Long faGlAccount;

    @NotNull(message = "Accumulation Account is required")
    private Long faAccumulationAccount;

    @NotNull(message = "Depreciation Account is required")
    private Long faDepreciationAccount;

    @NotNull(message = "Cost Center is required")
    private Long costCenter;

    private List<String> userRolePoid;

    private Integer seqNo;

    @Size(max = 1, message = "active must be at most 1 character")
    private String active = "Y";
}
