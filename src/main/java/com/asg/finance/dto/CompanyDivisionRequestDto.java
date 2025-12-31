package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyDivisionRequestDto {
    @NotNull(message = "Division POID is required")
    private Long divPoid;
    private Long detRowId;
    private String remarks;
    private String logoImageBase64;
    private String companyDivAddress;
    private String divisionName;
    private String companyDivAddressPos;
    @NotNull(message = "Action type is required")
    private String actionType;
}
