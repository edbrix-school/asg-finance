package com.asg.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReallocationDetailRequest {
	
	@NotNull(message = "rowId is required")
	private Long detRowId;

    @NotNull(message = "Company is required")
    private Long company;
    
    private String companyName;

    private BigDecimal sh;

    private BigDecimal ff;

    private BigDecimal ffs;

    private BigDecimal ffp;

    private BigDecimal properties;

    private BigDecimal mta;

    private BigDecimal pda;

    private BigDecimal admin;

    private BigDecimal total; // Optional - may be calculated

    @Size(max = 100, message = "Remarks must not exceed 100 characters")
    private String remarks;
}

