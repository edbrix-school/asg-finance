package com.asg.finance.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BankReconciliationRequest {

	@NotNull(message = "Transaction Group Poid is required")
	@Positive(message = "Invalid Transaction Group Poid")
	private Long transactionGroupPoid;
	@NotNull(message = "Transaction Company Poid is required")
	@Positive(message = "Invalid Transaction Company Poid")
	private Long transactionCompanyPoid;
	private String docId;
	@NotNull(message = "Transaction Poid is required")
	@Positive(message = "Invalid Transaction Poid")
	private Long transactionPoid;
	@NotNull(message = "Transaction date is required")
	private LocalDate transactionDate;
	@NotNull(message = "DocRef is required")
	private String docRef;
	private String chequeRef;
	private Long detRowId;
	private String narration;
	@NotNull(message = "GlCompany Poid is required")
	@Positive(message = "Invalid GLCompany Poid")
	private Long glCompanyPoid;
	@NotNull(message = "GlPoid is required")
	@Positive(message = "Invalid GlPoid")
	private Long glPoid;
	private Double drAmt;
	private Double crAmt;
	private Long postedBy;
	private LocalDate clearanceDate;
	private String userAuto;
}