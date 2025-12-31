package com.asg.finance.dto.masters;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceVehicleDetailRequestDto {
    
    @NotNull(message = "Amount is mandatory")
    private BigDecimal amount;
    
    private String remarks;
}