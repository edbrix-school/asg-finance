package com.asg.finance.dto;

import lombok.Data;

@Data
public class BeforePrintResponse {
	private String result;
	private String nextChequeNumber;
	private String defaultPrinter;
}