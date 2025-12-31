package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFixedAssetRequest {
    @NotBlank(message = "FA Code is required")
    private String faCode;
    
    @NotBlank(message = "FA Description is required")
    private String faDescription;
    
    @NotNull(message = "FA Category is required")
    private Long faCategory;
    
    @NotBlank(message = "Asset Type is required")
    private String assetType;
    
    @NotNull(message = "Asset Value is required")
    private BigDecimal assetValue;
}
