package com.asg.finance.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class BankReconciliationResponse {
	private Long transactionGroupPoid;
	private Long transactionCompanyPoid;
	private String docId; 
	private String docId1; 
	private Long transactionPoid; 
	private Date transactionDate; 
	private String docRef; 
	private String chequeRef;
	private Long detRowId;
	private String narration;
	private Long glCompanyPoid;
	private Long glPoid;
	private BigDecimal crAmt;
	private BigDecimal drAmt; 
}