package com.asg.finance.dto;

import lombok.Data;

@Data
public class AfterPrintRequest {
	private Long loginGroupPoid;
	private String loginUser;
	private Long loginCompanyPoid;
	private Long transactionPoid;
	private Long bankPoid;
	private String chqSignType;
	private Long loginUserPoid;
}
