package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalLedgerDto {
    private String glType = "TRADE_SUPPLIER";

    @NotBlank(message = "requestedBy is required")
    private String requestedBy;
}
