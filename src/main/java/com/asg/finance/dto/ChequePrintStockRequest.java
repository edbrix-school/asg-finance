package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChequePrintStockRequest {
    @NotNull(message = "stock companyPoid is required")
    private Long companyPoid;

    @NotNull(message = "stock bankPoid is required")
    private Long bankPoid;

    @NotBlank(message = "stockType is required")
    private String stockType;

    @NotNull(message = "stock selected is required")
    private Boolean selected;
}
