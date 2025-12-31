package com.asg.finance.dto.masters;

import jakarta.validation.constraints.NotBlank;
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
public class InsuranceDetailRequestDto {
    
    @NotBlank(message = "Details is mandatory")
    private String details;
    
    @NotNull(message = "Amount is mandatory")
    private BigDecimal amount;
    
    private String remarks;
}