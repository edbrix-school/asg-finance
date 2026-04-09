package com.asg.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BankReconcHoldAndUholdRequest {

	@NotNull(message = "Transaction Group POID is required")
	@Positive(message = "Invalid Transaction Group POID ")
	private Long transactionGroupPoid;

	@NotNull(message = "Transaction Company POID is required")
	@Positive(message = "Invalid Transaction Company POID")
	private Long transactionCompanyPoid;

	@NotBlank(message = "Document ID is required")
	private String docId;

	@NotNull(message = "Transaction POID is required")
	@Positive(message = "Invalid Transaction POID")
	private Long transactionPoid;

	@NotNull(message = "User POID is required")
	@Positive(message = "Invalid User POID")
	private Long userPoid;

	@NotBlank(message = "Document Reference is required")
	@NotNull(message = "Document Reference is required")
	private String docRef;
}
