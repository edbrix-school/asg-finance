package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationProcessedXlDetail {

    private Long transactionPoid;

    @NotNull(message = "Company is required")
    private Long company;

    @NotNull(message = "CompanyCode is required")
    private String companyCode;

    private Long detRowId;

    @NotBlank(message = "Cost Centre is required")
    @Size(max = 100, message = "Cost Centre must not exceed 100 characters")
    private String costCentre;

    @NotNull(message = "Percent is required")
    private BigDecimal percent;

    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    private String remarks;

    private String actionType; // For tracking create/update/delete actions
}
