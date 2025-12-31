package com.asg.finance.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class PendingChequeResponse {
	private Long transactionPoid;
	private String company;
	private Long companyPoid;
	private Long bankPoid;
	private String partyName;
	private String bank;
	private String pvNo;
	private Date docDate;
	private Date chqDate;
	private BigDecimal chequeAmount;
	private String chqSignType;
	private String chqCardNo;
	private String accountPayee;
}
