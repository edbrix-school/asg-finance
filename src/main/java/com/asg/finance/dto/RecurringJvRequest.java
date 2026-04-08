package com.asg.finance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RecurringJvRequest {
    
    @NotBlank(message = "Narration is mandatory")
    private String narration;

    private LocalDate  transactionDate;
    
    @NotNull(message = "Start date is mandatory")
    private LocalDate startDate;
    
    @NotNull(message = "Total amount is mandatory")
    @DecimalMin(value = "0.001", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Number of months is mandatory")
    @Min(value = 1, message = "Number of months must be at least 1")
    private Integer noOfMonths;
    
    @NotBlank(message = "Ref type is mandatory")
    private String refType;
    
    private Long employeePoid;
    private Long assetPoid;
    private String policyNumber;
    private String remarks;
    private BigDecimal monthWiseAmount;

    @NotEmpty(message = "At least one detail line is required")
    private List<RecurringJvDetailRequest> details;

    private List<RecurringJVScheduleWiseDetailsDto> scheduleDetails;
}
