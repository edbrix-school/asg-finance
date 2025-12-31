package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierImportRequestDto {
    @NotNull
    private Long transactionPoid;
    @NotNull
    private Long groupPoid;
    @NotNull
    private Long companyPoid;
    @NotNull
    private Long userPoid;
}