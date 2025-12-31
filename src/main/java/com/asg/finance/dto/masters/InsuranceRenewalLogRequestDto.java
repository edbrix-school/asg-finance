package com.asg.finance.dto.masters;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class InsuranceRenewalLogRequestDto {
    
    @NotNull(message = "Renewal Date is mandatory")
    private LocalDate renewalDate;
    
    @NotNull(message = "From Date is mandatory")
    private LocalDate fromDate;

    @NotNull(message = "To Date is mandatory")
    private LocalDate expiryDate;

    private BigDecimal insuranceAmount;
    
    private BigDecimal premiumAmount;
}