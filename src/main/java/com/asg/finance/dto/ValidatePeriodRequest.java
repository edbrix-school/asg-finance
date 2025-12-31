package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePeriodRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Period From is required")
    private Timestamp periodFrom;

    @NotNull(message = "Period To is required")
    private Timestamp periodTo;
}


