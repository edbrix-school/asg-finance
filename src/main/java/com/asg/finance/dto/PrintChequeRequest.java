package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrintChequeRequest {
	@NotNull(message = "Bank POID is required")
	private Long bankPoid;
	@NotNull(message = "Company POID is required")
	private String CompanyPoid;
	@NotBlank(message = "Stock Type is required")
	@Size(max = 20, message = "Stock Type must not exceed 20 characters")
	private String chqSignType;
	private Long GroupPoid;
	private String User;
	private Long transactionPoid;
	private String suppressBalanceCheck;
	private Long UserPoid;
}