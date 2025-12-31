package com.asg.finance.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ChequeStockResponse {
	private String company;
	private String bank;
	private String stockType;
	private Integer totalChq;
	private String fromChq;
	private String toChq;
	private String currentChq;
	private String lastChq;
	private Boolean selected;
	private String bankPoid;
	private String companyPoid;
	private BigDecimal availableBal;
	private Integer chqCount;
}
