package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChequePrintBatchRequest {
    @NotNull(message = "stocks payload is required")
    private List<@Valid ChequePrintStockRequest> stocks;

    @NotNull(message = "pendingCheques payload is required")
    private List<@Valid ChequePrintPendingRequest> pendingCheques;

    private String suppressBalanceCheck;
}
