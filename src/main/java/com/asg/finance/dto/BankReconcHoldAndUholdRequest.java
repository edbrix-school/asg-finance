package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BankReconcHoldAndUholdRequest {

	@NotNull(message = "Transaction Group POID is required")
	private Long transactionGroupPoid;

	@NotNull(message = "Transaction Company POID is required")
	private Long transactionCompanyPoid;

	@NotBlank(message = "Document ID is required")
	private String docId;

	@NotNull(message = "Transaction POID is required")
	private Long transactionPoid;
	
	@NotNull(message = "User POID is required")
	private Long userPoid;

	private String docRef;
}
