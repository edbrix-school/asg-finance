package com.asg.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankReconcileReportRow {
	private Long transactionGroupPoid;
	private Long transactionCompanyPoid;
	private String docId;
	private Long transactionPoid;
	private LocalDate transactionDate;
	private String docRef;
	private String chequeRef;
	private Long detRowId;
	private String narration;
	private Long glCompanyPoid;
	private Long glPoid;
	private BigDecimal crAmt;
	private BigDecimal drAmt;
	private LocalDate clearanceDate;
	private String chequeStatus;
	private String docId1;
}