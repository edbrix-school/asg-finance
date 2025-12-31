package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetDepreciationRequest {
    
    @NotNull(message = "Fixed Asset POID is required")
    private Long faPoid;
}