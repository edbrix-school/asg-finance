package com.asg.finance.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BankReconcileReportRequest {

	private Long groupPoid;
	private Long companyPoid;
	private Long bankPoid;
	private LocalDate dateFrom;
	private LocalDate dateTill;
	private String chequeNo;
	private String reconcileCheque;
	private String brType;
	private String chequeType;
	private String chequeFilter;
}
