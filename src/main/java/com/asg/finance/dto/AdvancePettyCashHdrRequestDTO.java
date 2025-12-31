package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvancePettyCashHdrRequestDTO {

    @NotNull(message = "Transaction date is mandatory")
    private LocalDate transactionDate;

    @NotNull(message = "Petty Cash GL POID is mandatory")
    private Long pettyCashGlPoid;

    @NotBlank(message = "Paying to is mandatory")
    @Size(max = 500, message = "Paying to must be at most 500 characters")
    private String payingTo;

    @DecimalMin(value = "0.0", inclusive = true, message = "IOU amount must be positive")
    private BigDecimal iouAmount;

    @Size(max = 1000, message = "Narration must be at most 1000 characters")
    private String narration;

    @Size(max = 100, message = "Status must be at most 100 characters")
    private String status;

    @Size(max = 1000, message = "Closed reason must be at most 1000 characters")
    private String closedReason;
}