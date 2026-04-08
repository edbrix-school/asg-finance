package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChequePrintPendingRequest {
    @NotNull(message = "transactionPoid is required")
    private Long transactionPoid;

    @NotNull(message = "companyPoid is required")
    private Long companyPoid;

    private String pvNo;

    @NotBlank(message = "accountPayee is required")
    private String accountPayee;

    @NotNull(message = "selected is required")
    private Boolean selected;
}
