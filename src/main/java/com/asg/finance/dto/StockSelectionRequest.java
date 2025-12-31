package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSelectionRequest {

    @NotNull(message = "Bank POID is required")
    private Long bankPoid;

    @NotNull(message = "Company POID is required")
    private Long companyPoid;

    @NotBlank(message = "Stock Type is required")
    @Size(max = 20, message = "Stock Type must not exceed 20 characters")
    private String signType;

    @NotNull(message = "Selected flag is required")
    private Boolean selected;

    @Size(max = 50, message = "Current cheque must not exceed 50 characters")
    private String currentCheque;
}
