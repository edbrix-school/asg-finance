package com.asg.finance.dto;

import java.util.Date;

import lombok.Data;

@Data
public class BankReconciliationRequest {

	private Long transactionGroupPoid;
	private Long transactionCompanyPoid;
	private String docId;
	private Long transactionPoid;
	private Date transactionDate;
	private String docRef;
	private String chequeRef;
	private Long detRowId;
	private String narration;
	private Long glCompanyPoid;
	private Long glPoid;
	private Double drAmt;
	private Double crAmt;
	private Long postedBy;
	private Date clearanceDate;
	private String userAuto;
}