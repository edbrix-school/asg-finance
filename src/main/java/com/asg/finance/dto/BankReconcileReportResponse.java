package com.asg.finance.dto;

import java.util.List;

import lombok.Data;

@Data
public class BankReconcileReportResponse {

	private List<BankReconcileReportRow> reportData;

	private String openingBalance;
	private String closingBalance;
	private String creditTotal;
	private String debitTotal;
	private String unclearBalance;
	private String debitTotal2;
	private String extraValue;
}
