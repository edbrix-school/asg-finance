package com.asg.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateScheduleRequest {
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Number of months is required")
    @Positive(message = "Number of months must be positive")
    private Integer noOfMonths;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @Valid
    @NotNull(message = "Details are required")
    private List<RecurringJvDetailRequest> details;
}