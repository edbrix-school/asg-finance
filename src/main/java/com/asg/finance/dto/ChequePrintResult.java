package com.asg.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChequePrintResult {

	private Long transactionPoid;
	private String status;
	private String message;
	private String chequeNumber;
	private String printer;
}
